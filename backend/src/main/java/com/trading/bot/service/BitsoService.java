package com.trading.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.TickerDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class BitsoService {

    private static final Duration TICKER_TTL = Duration.ofSeconds(10);

    @Value("${bitso.api.url}")
    private String bitsoApiUrl;

    @Value("${binance.api.url}")
    private String binanceApiUrl;

    private TickerDTO cachedTicker;
    private Instant cachedTickerAt = Instant.EPOCH;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BitsoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /** Ticker con caché de 10 s: /trades, /portfolio y /ohlcv lo piden en la misma ronda. */
    public synchronized TickerDTO getTicker() {
        if (cachedTicker != null && Instant.now().isBefore(cachedTickerAt.plus(TICKER_TTL))) {
            return cachedTicker;
        }
        cachedTicker = fetchTicker();
        cachedTickerAt = Instant.now();
        return cachedTicker;
    }

    private TickerDTO fetchTicker() {
        try {
            String url = bitsoApiUrl + "/ticker/?book=btc_mxn";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Bitso ticker API returned status: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode payload = root.get("payload");

            double last = parseDouble(payload.get("last").asText());
            double ask = parseDouble(payload.get("ask").asText());
            double bid = parseDouble(payload.get("bid").asText());
            double volume = parseDouble(payload.get("volume").asText());
            // Bitso manda change_24 en pesos; el frontend espera el porcentaje
            double change24Mxn = payload.has("change_24") ? parseDouble(payload.get("change_24").asText()) : 0.0;
            double change24h = percentChange(last, change24Mxn);

            return new TickerDTO(last, ask, bid, volume, change24h);

        } catch (Exception e) {
            throw new RuntimeException("Error fetching ticker from Bitso: " + e.getMessage(), e);
        }
    }

    public List<OhlcvCandleDTO> getOhlcv(int limit) {
        try {
            // Fetch OHLCV from Binance (BTC/USDT, 1h candles)
            String url = binanceApiUrl + "/api/v3/klines?symbol=BTCUSDT&interval=1h&limit=" + limit;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Binance OHLCV API returned status: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                throw new RuntimeException("Binance OHLCV API returned no candles");
            }

            // Tipo de cambio implícito: último precio en Bitso (MXN) entre el último cierre en Binance (USDT)
            double lastCloseUsd = parseDouble(root.get(root.size() - 1).get(4).asText());
            if (lastCloseUsd <= 0) {
                throw new RuntimeException("Binance OHLCV API returned an invalid close price");
            }
            double usdToMxn = getTicker().last() / lastCloseUsd;

            List<OhlcvCandleDTO> candles = new ArrayList<>();
            for (JsonNode node : root) {
                // Binance usa milisegundos; el DTO y el frontend usan segundos
                long timestamp = node.get(0).asLong() / 1000;
                double open  = parseDouble(node.get(1).asText()) * usdToMxn;
                double high  = parseDouble(node.get(2).asText()) * usdToMxn;
                double low   = parseDouble(node.get(3).asText()) * usdToMxn;
                double close = parseDouble(node.get(4).asText()) * usdToMxn;
                double volume = parseDouble(node.get(5).asText());
                candles.add(new OhlcvCandleDTO(timestamp, open, high, low, close, volume));
            }

            return candles;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching OHLCV: " + e.getMessage(), e);
        }
    }

    /** Cambio porcentual a partir del último precio y del cambio absoluto en 24 h. */
    static double percentChange(double last, double absoluteChange) {
        double open = last - absoluteChange;
        return open > 0 ? (absoluteChange / open) * 100 : 0.0;
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
