package com.trading.bot.client;

import com.trading.bot.exception.UpstreamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BinanceClientTest {

    private MockRestServiceServer server;
    private BinanceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://data-api.binance.vision");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BinanceClient(builder.build(), Duration.ZERO);
    }

    @Test
    void convertsMillisecondsToSeconds() {
        server.expect(requestTo("https://data-api.binance.vision/api/v3/klines?symbol=BTCUSDT&interval=1h&limit=2"))
                .andRespond(withSuccess("""
                        [[1700000000000,"100","110","90","105","3.5",0,"0",0,"0","0","0"],
                         [1700003600000,"105","120","100","115","4.0",0,"0",0,"0","0","0"]]""", APPLICATION_JSON));

        var candles = client.fetchHourlyCandlesUsd(2);

        assertThat(candles).hasSize(2);
        assertThat(candles.get(0).timestamp()).isEqualTo(1_700_000_000L);
        assertThat(candles.get(1).close()).isEqualTo(115.0);
        assertThat(candles.get(1).volume()).isEqualTo(4.0);
    }

    @Test
    void rejectsAnEmptyAnswer() {
        server.expect(requestTo("https://data-api.binance.vision/api/v3/klines?symbol=BTCUSDT&interval=1h&limit=5"))
                .andRespond(withSuccess("[]", APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchHourlyCandlesUsd(5)).isInstanceOf(UpstreamException.class);
    }

    @Test
    void rejectsALimitAboveWhatBinanceAccepts() {
        assertThatThrownBy(() -> client.fetchHourlyCandlesUsd(1001)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.fetchHourlyCandlesUsd(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
