package com.trading.bot.client;

import com.trading.bot.exception.UpstreamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BitsoClientTest {

    private static final String URL = "https://api.bitso.com/v3/ticker/?book=btc_mxn";
    private static final String TICKER = """
            {"success":true,"payload":{"last":"1500000","ask":"1500100","bid":"1499900",
             "volume":"12.5","change_24":"30000"}}""";

    private MockRestServiceServer server;
    private BitsoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.bitso.com/v3");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BitsoClient(builder.build(), Duration.ZERO);
    }

    @Test
    void parsesTheTickerAndTurnsTheChangeIntoAPercentage() {
        server.expect(requestTo(URL)).andRespond(withSuccess(TICKER, APPLICATION_JSON));

        var ticker = client.fetchTicker();

        assertThat(ticker.last()).isEqualTo(1_500_000);
        assertThat(ticker.ask()).isEqualTo(1_500_100);
        // Subió 30 000 desde 1 470 000
        assertThat(ticker.change24h()).isCloseTo(2.0408, within(1e-4));
        server.verify();
    }

    @Test
    void retriesAfterTooManyRequests() {
        server.expect(requestTo(URL)).andRespond(withTooManyRequests());
        server.expect(requestTo(URL)).andRespond(withSuccess(TICKER, APPLICATION_JSON));

        assertThat(client.fetchTicker().last()).isEqualTo(1_500_000);
        server.verify();
    }

    @Test
    void givesUpAfterThreeServerErrors() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(URL)).andRespond(withServerError());
        }

        assertThatThrownBy(client::fetchTicker)
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("Bitso")
                .hasMessageContaining("500");
        server.verify();
    }

    @Test
    void doesNotRetryAClientError() {
        server.expect(requestTo(URL)).andRespond(withBadRequest());

        assertThatThrownBy(client::fetchTicker).isInstanceOf(UpstreamException.class);
        server.verify();
    }

    @Test
    void rejectsATickerWithoutPrice() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"payload\":{}}", APPLICATION_JSON));

        assertThatThrownBy(client::fetchTicker)
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("precio");
    }

    @Test
    void percentChangeUsesThePriceFrom24hAgoAsBase() {
        assertThat(BitsoClient.percentChange(110, 10)).isCloseTo(10.0, within(1e-9));
        assertThat(BitsoClient.percentChange(90, -10)).isCloseTo(-10.0, within(1e-9));
        assertThat(BitsoClient.percentChange(10, 10)).isZero();
    }
}
