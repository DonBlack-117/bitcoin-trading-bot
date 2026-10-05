package com.trading.bot.client;

import com.trading.bot.exception.ConfigMissingException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CoinMarketCapClientTest {

    @Test
    void failsClearlyWithoutApiKey() {
        var client = new CoinMarketCapClient(RestClient.create(), " ", Duration.ZERO);

        assertThatThrownBy(() -> client.listings(10, "MXN"))
                .isInstanceOf(ConfigMissingException.class)
                .hasMessageContaining("CMC_API_KEY");
    }

    @Test
    void sendsTheKeyAndReturnsTheDataNode() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://pro-api.coinmarketcap.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var client = new CoinMarketCapClient(builder.build(), "clave-de-prueba", Duration.ZERO);

        server.expect(requestTo("https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest?limit=10&convert=MXN"))
                .andExpect(header("X-CMC_PRO_API_KEY", "clave-de-prueba"))
                .andRespond(withSuccess("{\"data\":[{\"symbol\":\"BTC\"}]}", APPLICATION_JSON));

        assertThat(client.listings(10, "MXN").get(0).get("symbol").asText()).isEqualTo("BTC");
        server.verify();
    }
}
