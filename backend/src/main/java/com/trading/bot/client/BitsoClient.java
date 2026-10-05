package com.trading.bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.trading.bot.dto.TickerDTO;
import com.trading.bot.exception.UpstreamException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Ticker BTC/MXN de Bitso (precio, compra, venta, volumen y cambio en 24 h). */
@Component
public class BitsoClient {

    private final RestClient restClient;
    private final UpstreamCall upstream;

    @Autowired
    public BitsoClient(@Qualifier("bitsoRestClient") RestClient restClient) {
        this(restClient, Duration.ofMillis(300));
    }

    BitsoClient(RestClient restClient, Duration retryDelay) {
        this.restClient = restClient;
        this.upstream = new UpstreamCall("Bitso", retryDelay);
    }

    public TickerDTO fetchTicker() {
        JsonNode root = upstream.execute(() -> restClient.get()
                .uri("/ticker/?book=btc_mxn")
                .retrieve()
                .body(JsonNode.class));

        JsonNode payload = root == null ? null : root.get("payload");
        if (payload == null || !payload.hasNonNull("last")) {
            throw new UpstreamException("Bitso", "el ticker no trae precio");
        }
        double last = number(payload, "last");
        if (last <= 0) {
            throw new UpstreamException("Bitso", "precio inválido: " + last);
        }
        // Bitso manda change_24 en pesos; el frontend espera el porcentaje
        double change24h = percentChange(last, number(payload, "change_24"));
        return new TickerDTO(last, number(payload, "ask"), number(payload, "bid"),
                number(payload, "volume"), change24h);
    }

    /** Cambio porcentual a partir del último precio y del cambio absoluto en 24 h. */
    static double percentChange(double last, double absoluteChange) {
        double open = last - absoluteChange;
        return open > 0 ? (absoluteChange / open) * 100 : 0.0;
    }

    private static double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) return 0.0;
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException e) {
            throw new UpstreamException("Bitso", "valor no numérico en " + field);
        }
    }
}
