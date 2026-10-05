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

    @Bean
    RestClient bitsoRestClient(RestClient.Builder builder, @Value("${bitso.api.url}") String baseUrl) {
        return build(builder, baseUrl);
    }

    @Bean
    RestClient binanceRestClient(RestClient.Builder builder, @Value("${binance.api.url}") String baseUrl) {
        return build(builder, baseUrl);
    }

    @Bean
    RestClient cmcRestClient(RestClient.Builder builder, @Value("${cmc.api.url}") String baseUrl) {
        return build(builder, baseUrl);
    }

    private static RestClient build(RestClient.Builder builder, String baseUrl) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(READ_TIMEOUT);
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
