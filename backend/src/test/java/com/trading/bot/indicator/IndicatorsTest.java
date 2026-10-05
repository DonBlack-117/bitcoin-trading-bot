package com.trading.bot.indicator;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class IndicatorsTest {

    private static List<Double> range(int from, int to) {
        return IntStream.rangeClosed(from, to).mapToObj(i -> (double) i).toList();
    }

    @Test
    void smaAveragesEachWindow() {
        assertThat(Indicators.sma(range(1, 5), 3)).containsExactly(2.0, 3.0, 4.0);
    }

    @Test
    void emaStartsWithTheSmaAndFollowsTheTrend() {
        List<Double> ema = Indicators.ema(range(1, 10), 3);
        assertThat(ema).hasSize(8);
        assertThat(ema.get(0)).isEqualTo(2.0);
        // En una recta con paso 1, la EMA(3) se queda 1 por debajo del precio
        assertThat(ema.get(ema.size() - 1)).isCloseTo(9.0, within(0.01));
    }

    @Test
    void rsiIsHundredWhenPricesOnlyRiseAndZeroWhenTheyOnlyFall() {
        assertThat(Indicators.rsi(range(1, 30), 14)).allSatisfy(v -> assertThat(v).isEqualTo(100.0));

        List<Double> falling = new java.util.ArrayList<>(range(1, 30));
        Collections.reverse(falling);
        assertThat(Indicators.rsi(falling, 14)).allSatisfy(v -> assertThat(v).isEqualTo(0.0));
    }

    @Test
    void rsiIsFiftyWhenGainsAndLossesAreEqual() {
        List<Double> zigzag = IntStream.range(0, 40).mapToObj(i -> i % 2 == 0 ? 100.0 : 101.0).toList();
        List<Double> rsi = Indicators.rsi(zigzag, 14);
        assertThat(rsi).hasSize(40 - 14);
        assertThat(rsi.get(rsi.size() - 1)).isCloseTo(50.0, within(5.0));
    }

    @Test
    void bollingerBandsCollapseOnAFlatPrice() {
        List<Indicators.Band> bands = Indicators.bollinger(Collections.nCopies(25, 10.0), 20);
        assertThat(bands).hasSize(6);
        assertThat(bands).allSatisfy(b -> {
            assertThat(b.upper()).isEqualTo(10.0);
            assertThat(b.lower()).isEqualTo(10.0);
        });
    }

    @Test
    void obvAddsVolumeOnUpCandlesAndSubtractsOnDownCandles() {
        List<Double> obv = Indicators.obv(List.of(10.0, 11.0, 11.0, 9.0), List.of(5.0, 2.0, 3.0, 4.0));
        assertThat(obv).containsExactly(5.0, 7.0, 7.0, 3.0);
    }

    @Test
    void vwapWeightsTheTypicalPriceByVolume() {
        double vwap = Indicators.vwap(List.of(12.0, 22.0), List.of(8.0, 18.0), List.of(10.0, 20.0),
                List.of(1.0, 3.0), 24);
        assertThat(vwap).isCloseTo((10.0 * 1 + 20.0 * 3) / 4, within(1e-9));
    }

    @Test
    void atrFallsBackToOnePercentWithFewCandles() {
        assertThat(Indicators.atr(List.of(101.0), List.of(99.0), List.of(100.0), 14)).isEqualTo(1.0);
    }
}
