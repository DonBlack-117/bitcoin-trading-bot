package com.trading.bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.exception.UpstreamException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Velas de 1 h de BTC/USDT. Bitso no publica velas, por eso se usan las de Binance. */
@Component
public class BinanceClient {

    /** Máximo que acepta /api/v3/klines. */
    public static final int MAX_LIMIT = 1000;

    private final RestClient restClient;
    private final UpstreamCall upstream;

    @Autowired
    public BinanceClient(@Qualifier("binanceRestClient") RestClient restClient) {
        this(restClient, Duration.ofMillis(300));
    }

    BinanceClient(RestClient restClient, Duration retryDelay) {
        this.restClient = restClient;
        this.upstream = new UpstreamCall("Binance", retryDelay);
    }

    /** Velas en USDT, de la más antigua a la más reciente, con timestamp en segundos. */
    public List<OhlcvCandleDTO> fetchHourlyCandlesUsd(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit debe estar entre 1 y " + MAX_LIMIT);
        }
        JsonNode root = upstream.execute(() -> restClient.get()
                .uri(uri -> uri.path("/api/v3/klines")
                        .queryParam("symbol", "BTCUSDT")
                        .queryParam("interval", "1h")
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(JsonNode.class));

        if (root == null || !root.isArray() || root.isEmpty()) {
            throw new UpstreamException("Binance", "no devolvió velas");
        }
        List<OhlcvCandleDTO> candles = new ArrayList<>(root.size());
        for (JsonNode node : root) {
            // Binance usa milisegundos; el DTO y el frontend usan segundos
            candles.add(new OhlcvCandleDTO(
                    node.get(0).asLong() / 1000,
                    node.get(1).asDouble(),
                    node.get(2).asDouble(),
                    node.get(3).asDouble(),
                    node.get(4).asDouble(),
                    node.get(5).asDouble()));
        }
        if (candles.get(candles.size() - 1).close() <= 0) {
            throw new UpstreamException("Binance", "el último cierre no es válido");
        }
        return candles;
    }
}
