package com.trading.bot.service;

import com.trading.bot.client.BinanceClient;
import com.trading.bot.client.BitsoClient;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.TickerDTO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

class MarketDataServiceTest {

    private final BitsoClient bitso = mock(BitsoClient.class);
    private final BinanceClient binance = mock(BinanceClient.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-04T18:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final MarketDataService service = new MarketDataService(bitso, binance, clock);

    private static List<OhlcvCandleDTO> usdCandles(int count) {
        List<OhlcvCandleDTO> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double close = 60_000 + i * 10;
            candles.add(new OhlcvCandleDTO(1_700_000_000L + i * 3600L, close - 5, close + 20, close - 20, close, 2));
        }
        return candles;
    }

    @Test
    void cachesTheTickerForTenSeconds() {
        when(bitso.fetchTicker()).thenReturn(new TickerDTO(1_200_000, 0, 0, 0, 0));

        service.getTicker();
        now.set(now.get().plus(Duration.ofSeconds(9)));
        service.getTicker();
        verify(bitso, times(1)).fetchTicker();

        now.set(now.get().plus(Duration.ofSeconds(1)));
        service.getTicker();
        verify(bitso, times(2)).fetchTicker();
    }

    @Test
    void convertsCandlesSoTheLastCloseMatchesBitso() {
        List<OhlcvCandleDTO> usd = usdCandles(3);   // último cierre: 60 020 USD
        when(binance.fetchHourlyCandlesUsd(3)).thenReturn(usd);
        when(bitso.fetchTicker()).thenReturn(new TickerDTO(1_200_400, 0, 0, 0, 0));

        List<OhlcvCandleDTO> mxn = service.getCandlesMxn(3);

        assertThat(mxn.get(2).close()).isCloseTo(1_200_400, within(1e-6));
        assertThat(mxn.get(0).close()).isCloseTo(60_000 * (1_200_400 / 60_020.0), within(1e-6));
        assertThat(mxn.get(0).volume()).isEqualTo(2.0);
    }

    @Test
    void chartSeriesLineUpWithTheirCandles() {
        when(binance.fetchHourlyCandlesUsd(60)).thenReturn(usdCandles(60));
        when(bitso.fetchTicker()).thenReturn(new TickerDTO(1_200_000, 0, 0, 0, 0));

        var chart = service.getChart(60);

        assertThat(chart.candles()).hasSize(60);
        assertThat(chart.rsi()).hasSize(60 - 14);
        assertThat(chart.rsi().get(0).timestamp()).isEqualTo(chart.candles().get(14).timestamp());
        assertThat(chart.bollinger()).hasSize(60 - 19);
        assertThat(chart.bollinger().get(0).timestamp()).isEqualTo(chart.candles().get(19).timestamp());
        assertThat(chart.bollinger()).allSatisfy(b -> assertThat(b.upper()).isGreaterThanOrEqualTo(b.lower()));
    }

    @Test
    void aBitsoFailureIsAlsoCachedForTenSeconds() {
        when(bitso.fetchTicker()).thenThrow(new com.trading.bot.exception.UpstreamException("Bitso", "HTTP 500"));

        for (int i = 0; i < 3; i++) {
            org.assertj.core.api.Assertions.assertThatThrownBy(service::getTicker).hasMessage("Bitso: HTTP 500");
        }
        verify(bitso, times(1)).fetchTicker();

        now.set(now.get().plus(Duration.ofSeconds(10)));
        reset(bitso);
        when(bitso.fetchTicker()).thenReturn(new TickerDTO(1_200_000, 0, 0, 0, 0));
        assertThat(service.getTicker().last()).isEqualTo(1_200_000);
    }
}
