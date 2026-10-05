package com.trading.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trading.bot.client.CoinMarketCapClient;
import com.trading.bot.dto.CryptoDTO;
import com.trading.bot.dto.GlobalMarketDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Mercado global y listados de CoinMarketCap, con caché de 5 min (spring.cache.caffeine.spec). */
@Service
public class CoinMarketCapService {

    private final CoinMarketCapClient client;

    public CoinMarketCapService(CoinMarketCapClient client) {
        this.client = client;
    }

    @Cacheable("globalMarket")
    public GlobalMarketDTO getGlobalMarket() {
        JsonNode listings = client.listings(20, "USD");
        JsonNode global = client.globalMetrics("USD");
        JsonNode globalQuote = global.path("quote").path("USD");

        List<CryptoDTO> topCryptos = parseList(listings, "USD");
        double avgPct24h = topCryptos.stream()
                .filter(c -> c.pct24h() != null)
                .mapToDouble(CryptoDTO::pct24h)
                .average().orElse(0);

        // Aproximación propia del índice de miedo y codicia: promedio del cambio en 24 h del top 20
        int fearGreedScore = (int) Math.max(0, Math.min(100, 50 + avgPct24h * 2));

        return new GlobalMarketDTO(
                globalQuote.path("total_market_cap").asDouble(),
                globalQuote.path("total_volume_24h").asDouble(),
                global.path("btc_dominance").asDouble(),
                global.path("eth_dominance").asDouble(),
                global.path("active_cryptocurrencies").asLong(),
                global.path("active_exchanges").asLong(0),
                globalQuote.path("total_market_cap_yesterday_percentage_change").asDouble(0.0),
                topCryptos,
                fearGreedScore,
                fearGreedLabel(fearGreedScore));
    }

    /** Top N criptomonedas con precios en la moneda pedida, p. ej. "MXN" o "USD". */
    @Cacheable(value = "cryptoList", key = "#limit + '-' + #convert")
    public List<CryptoDTO> getCryptoList(int limit, String convert) {
        return parseList(client.listings(limit, convert), convert);
    }

    static String fearGreedLabel(int score) {
        if (score < 25) return "Extreme Fear";
        if (score < 45) return "Fear";
        if (score < 55) return "Neutral";
        if (score < 75) return "Greed";
        return "Extreme Greed";
    }

    private static List<CryptoDTO> parseList(JsonNode data, String convert) {
        List<CryptoDTO> list = new ArrayList<>();
        for (JsonNode crypto : data) {
            JsonNode quote = crypto.path("quote").path(convert);
            if (quote.isMissingNode()) continue;
            list.add(new CryptoDTO(
                    crypto.path("cmc_rank").asInt(),
                    crypto.path("symbol").asText(),
                    crypto.path("name").asText(),
                    quote.path("price").asDouble(),
                    quote.path("market_cap").asDouble(),
                    quote.path("volume_24h").asDouble(),
                    nullableDouble(quote, "percent_change_1h"),
                    nullableDouble(quote, "percent_change_24h"),
                    nullableDouble(quote, "percent_change_7d")));
        }
        return list;
    }

    private static Double nullableDouble(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }
}
