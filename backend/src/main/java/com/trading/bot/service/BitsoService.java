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
import java.util.ArrayList;
import java.util.List;

@Service
public class BitsoService {

    @Value("${bitso.api.url}")
    private String bitsoApiUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BitsoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public TickerDTO getTicker() {
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
            double change24h = payload.has("change_24") ? payload.get("change_24").asDouble() : 0.0;

            return new TickerDTO(last, ask, bid, volume, change24h);

        } catch (Exception e) {
            throw new RuntimeException("Error fetching ticker from Bitso: " + e.getMessage(), e);
        }
    }

    public List<OhlcvCandleDTO> getOhlcv(int limit) {
        try {
            // Get MXN/USD rate from Bitso ticker
            TickerDTO ticker = getTicker();
            double usdToMxn = ticker.last() / getBtcUsd();

            // Fetch OHLCV from Binance (BTC/USDT, 1h candles)
            String url = "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1h&limit=" + limit;
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
            List<OhlcvCandleDTO> candles = new ArrayList<>();
            for (JsonNode node : root) {
                long timestamp = node.get(0).asLong();
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

    private double getBtcUsd() {
        try {
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            return parseDouble(root.get("price").asText());
        } catch (Exception e) {
            return 85000.0; // fallback
        }
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
