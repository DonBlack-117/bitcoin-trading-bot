package com.trading.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.bot.dto.CryptoDTO;
import com.trading.bot.dto.GlobalMarketDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class CoinMarketCapService {

    @Value("${cmc.api.url}")
    private String cmcApiUrl;

    @Value("${cmc.api.key}")
    private String cmcApiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CoinMarketCapService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Cacheable("globalMarket")
    public GlobalMarketDTO getGlobalMarket() {
        try {
            // Fetch listings
            String listingsUrl = cmcApiUrl + "/v1/cryptocurrency/listings/latest?limit=20&convert=USD";
            JsonNode listingsRoot = fetchJson(listingsUrl);

            // Fetch global metrics
            String globalUrl = cmcApiUrl + "/v1/global-metrics/quotes/latest?convert=USD";
            JsonNode globalRoot = fetchJson(globalUrl);

            // Parse global metrics
            JsonNode globalData = globalRoot.get("data");
            JsonNode globalQuote = globalData.get("quote").get("USD");

            double totalMarketCap = globalQuote.get("total_market_cap").asDouble();
            double totalVolume24h = globalQuote.get("total_volume_24h").asDouble();
            double btcDominance = globalData.get("btc_dominance").asDouble();
            double ethDominance = globalData.get("eth_dominance").asDouble();
            long activeCryptos = globalData.get("active_cryptocurrencies").asLong();
            long activeExchanges = globalData.has("active_exchanges") ? globalData.get("active_exchanges").asLong() : 0;
            double marketCapChange24h = globalQuote.has("total_market_cap_yesterday_percentage_change")
                    ? globalQuote.get("total_market_cap_yesterday_percentage_change").asDouble()
                    : 0.0;

            // Parse top cryptos
            JsonNode listingsData = listingsRoot.get("data");
            List<CryptoDTO> topCryptos = new ArrayList<>();
            double totalPct24h = 0;
            int count = 0;

            for (JsonNode crypto : listingsData) {
                int rank = crypto.get("cmc_rank").asInt();
                String symbol = crypto.get("symbol").asText();
                String name = crypto.get("name").asText();
                JsonNode quote = crypto.get("quote").get("USD");
                double price = quote.get("price").asDouble();
                double marketCap = quote.get("market_cap").asDouble();
                double volume24h = quote.get("volume_24h").asDouble();
                Double pct1h = quote.has("percent_change_1h") && !quote.get("percent_change_1h").isNull()
                        ? quote.get("percent_change_1h").asDouble() : null;
                Double pct24h = quote.has("percent_change_24h") && !quote.get("percent_change_24h").isNull()
                        ? quote.get("percent_change_24h").asDouble() : null;
                Double pct7d = quote.has("percent_change_7d") && !quote.get("percent_change_7d").isNull()
                        ? quote.get("percent_change_7d").asDouble() : null;

                topCryptos.add(new CryptoDTO(rank, symbol, name, price, marketCap, volume24h, pct1h, pct24h, pct7d));

                if (pct24h != null) {
                    totalPct24h += pct24h;
                    count++;
                }
            }

            // Fear & Greed proxy
            double avgPct24h = count > 0 ? totalPct24h / count : 0;
            int fearGreedScore = (int) Math.max(0, Math.min(100, 50 + avgPct24h * 2));

            String fearGreedLabel;
            String fearGreedColor;

            if (fearGreedScore < 25) {
                fearGreedLabel = "Extreme Fear";
                fearGreedColor = "#ef4444";
            } else if (fearGreedScore < 45) {
                fearGreedLabel = "Fear";
                fearGreedColor = "#f97316";
            } else if (fearGreedScore < 55) {
                fearGreedLabel = "Neutral";
                fearGreedColor = "#f59e0b";
            } else if (fearGreedScore < 75) {
                fearGreedLabel = "Greed";
                fearGreedColor = "#22c55e";
            } else {
                fearGreedLabel = "Extreme Greed";
                fearGreedColor = "#10b981";
            }

            return new GlobalMarketDTO(
                    totalMarketCap,
                    totalVolume24h,
                    btcDominance,
                    ethDominance,
                    activeCryptos,
                    activeExchanges,
                    marketCapChange24h,
                    topCryptos,
                    fearGreedScore,
                    fearGreedLabel,
                    fearGreedColor
            );

        } catch (Exception e) {
            throw new RuntimeException("Error fetching global market data from CoinMarketCap: " + e.getMessage(), e);
        }
    }

    /**
     * Returns top N cryptocurrencies with prices in the requested currency.
     * @param convert e.g. "MXN" or "USD"
     */
    @Cacheable(value = "cryptoList", key = "#limit + '-' + #convert")
    public List<CryptoDTO> getCryptoList(int limit, String convert) {
        try {
            String url = cmcApiUrl + "/v1/cryptocurrency/listings/latest?limit=" + limit + "&convert=" + convert;
            JsonNode root = fetchJson(url);
            JsonNode data = root.get("data");
            List<CryptoDTO> list = new ArrayList<>();
            for (JsonNode crypto : data) {
                int    rank    = crypto.get("cmc_rank").asInt();
                String symbol  = crypto.get("symbol").asText();
                String name    = crypto.get("name").asText();
                JsonNode quote = crypto.get("quote").get(convert);
                if (quote == null || quote.isMissingNode()) continue;
                double price     = quote.get("price").asDouble();
                double marketCap = quote.get("market_cap").asDouble();
                double volume24h = quote.get("volume_24h").asDouble();
                Double pct1h  = safeDouble(quote, "percent_change_1h");
                Double pct24h = safeDouble(quote, "percent_change_24h");
                Double pct7d  = safeDouble(quote, "percent_change_7d");
                list.add(new CryptoDTO(rank, symbol, name, price, marketCap, volume24h, pct1h, pct24h, pct7d));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching crypto list: " + e.getMessage(), e);
        }
    }

    private Double safeDouble(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) return node.get(field).asDouble();
        return null;
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("X-CMC_PRO_API_KEY", cmcApiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("CoinMarketCap API returned status: " + response.statusCode()
                    + " body: " + response.body());
        }

        return objectMapper.readTree(response.body());
    }
}
