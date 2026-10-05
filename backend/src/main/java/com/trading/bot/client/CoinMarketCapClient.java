package com.trading.bot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.trading.bot.exception.ConfigMissingException;
import com.trading.bot.exception.UpstreamException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Listados y métricas globales de CoinMarketCap. Necesita CMC_API_KEY. */
@Component
public class CoinMarketCapClient {

    private final RestClient restClient;
    private final String apiKey;
    private final UpstreamCall upstream;

    @Autowired
    public CoinMarketCapClient(@Qualifier("cmcRestClient") RestClient restClient,
                               @Value("${cmc.api.key:}") String apiKey) {
        this(restClient, apiKey, Duration.ofMillis(500));
    }

    CoinMarketCapClient(RestClient restClient, String apiKey, Duration retryDelay) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.upstream = new UpstreamCall("CoinMarketCap", retryDelay);
    }

    public JsonNode listings(int limit, String convert) {
        return get("/v1/cryptocurrency/listings/latest?limit={limit}&convert={convert}", limit, convert);
    }

    public JsonNode globalMetrics(String convert) {
        return get("/v1/global-metrics/quotes/latest?convert={convert}", convert);
    }

    private JsonNode get(String uri, Object... vars) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ConfigMissingException("Falta la variable de entorno CMC_API_KEY");
        }
        JsonNode root = upstream.execute(() -> restClient.get()
                .uri(uri, vars)
                .header("X-CMC_PRO_API_KEY", apiKey)
                .header("Accept", "application/json")
                .retrieve()
                .body(JsonNode.class));
        if (root == null || !root.has("data")) {
            throw new UpstreamException("CoinMarketCap", "respuesta sin datos");
        }
        return root.get("data");
    }
}
