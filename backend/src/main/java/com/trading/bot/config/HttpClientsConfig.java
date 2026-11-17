package com.trading.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Un RestClient por API externa, todos con timeout de conexión y de lectura. */
@Configuration
public class HttpClientsConfig {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);
    // El ticker de Bitso está en el camino de cada petición (tras tickerFetchLock): un timeout corto
    // evita que Bitso lento bloquee /trades, /portfolio y /chart hasta ~60 s.
    private static final Duration BITSO_READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    RestClient bitsoRestClient(
            RestClient.Builder builder, @Value("${bitso.api.url}") String baseUrl) {
        return build(builder, baseUrl, BITSO_READ_TIMEOUT);
    }

    @Bean
    RestClient binanceRestClient(
            RestClient.Builder builder, @Value("${binance.api.url}") String baseUrl) {
        return build(builder, baseUrl, READ_TIMEOUT);
    }

    @Bean
    RestClient cmcRestClient(RestClient.Builder builder, @Value("${cmc.api.url}") String baseUrl) {
        return build(builder, baseUrl, READ_TIMEOUT);
    }

    private static RestClient build(
            RestClient.Builder builder, String baseUrl, Duration readTimeout) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(readTimeout);
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
