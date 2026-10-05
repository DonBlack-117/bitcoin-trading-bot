package com.trading.bot.service;

import com.trading.bot.domain.SignalType;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class StrategyServiceTest {

    private final StrategyService strategy = new StrategyService(TestData.properties(), TestData.CLOCK);

    /** Velas de 1 h con cierres en línea recta: start + i * step. */
    private static List<OhlcvCandleDTO> linear(int count, double start, double step) {
        List<OhlcvCandleDTO> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double close = start + i * step;
            double open = close - step;
            candles.add(new OhlcvCandleDTO(i * 3600L, open, Math.max(open, close) + 1, Math.min(open, close) - 1, close, 10));
        }
        return candles;
    }

    @Test
    void rejectsFewerThan30Candles() {
        assertThrows(IllegalArgumentException.class, () -> strategy.calculateSignal(linear(29, 1000, 1)));
        assertThrows(IllegalArgumentException.class, () -> strategy.calculateSignal(null));
    }

    @Test
    void downtrendNearSupportVotesBuyOnSupportAndSellOnTrend() {
        SignalResponseDTO signal = strategy.calculateSignal(linear(200, 2000, -1));
        Map<String, Integer> votes = signal.votes();

        assertEquals(3, votes.get("soporteResistencia"), "el cierre es el mínimo: está sobre el soporte");
        assertEquals(-1, votes.get("emaCruce"), "precio < SMA 50 < SMA 200");
        assertEquals(-2, votes.get("volumenVwap"), "baja con volumen y bajo el VWAP");
        assertEquals(0.0, signal.rsi(), 1e-9, "sin velas alcistas el RSI es 0");
    }

    @Test
    void uptrendNearResistanceVotesSellOnResistanceAndBuyOnTrend() {
        SignalResponseDTO signal = strategy.calculateSignal(linear(200, 1000, 1));
        Map<String, Integer> votes = signal.votes();

        assertEquals(-3, votes.get("soporteResistencia"), "el cierre es el máximo: está en la resistencia");
        assertEquals(1, votes.get("emaCruce"), "precio > SMA 50 > SMA 200");
        assertEquals(2, votes.get("volumenVwap"), "sube con volumen y sobre el VWAP");
        assertEquals(100.0, signal.rsi(), 1e-9, "sin velas bajistas el RSI es 100");
    }

    @Test
    void scoresConfidenceAndSignalAreConsistentWithVotes() {
        Random random = new Random(42);
        for (int run = 0; run < 200; run++) {
            List<OhlcvCandleDTO> candles = new ArrayList<>();
            double price = 1_500_000;
            for (int i = 0; i < 200; i++) {
                double open = price;
                price = price * (1 + (random.nextDouble() - 0.5) * 0.02);
                candles.add(new OhlcvCandleDTO(i * 3600L, open, Math.max(open, price) * 1.002,
                        Math.min(open, price) * 0.998, price, 1 + random.nextDouble() * 20));
            }

            SignalResponseDTO s = strategy.calculateSignal(candles);
            int buy = s.votes().values().stream().filter(v -> v > 0).mapToInt(Integer::intValue).sum();
            int sell = s.votes().values().stream().filter(v -> v < 0).mapToInt(v -> -v).sum();

            assertEquals(buy, s.scoreBuy());
            assertEquals(sell, s.scoreSell());
            s.votes().values().forEach(v -> assertTrue(v >= -3 && v <= 3, "voto fuera de rango: " + v));
            assertTrue(s.confidence() >= 45 && s.confidence() <= 95, "confianza fuera de rango: " + s.confidence());
            assertEquals(price, s.price(), 1e-6);

            if (buy >= 6 && buy > sell) {
                assertEquals(SignalType.COMPRAR, s.signal());
            } else if (sell >= 6 && sell > buy) {
                assertEquals(SignalType.VENDER, s.signal());
            } else {
                assertTrue(s.confidence() <= 68, "una señal débil no debe pasar de 68%");
                assertFalse(s.actionable(), "el bot no opera con una señal débil");
            }
            assertEquals(s.signal() != SignalType.MANTENER && s.confidence() >= 60, s.actionable());
            assertEquals(TestData.CLOCK.instant(), s.calculatedAt());
        }
    }

    @Test
    void indicatorsIncludeLevelsUsedByTheDashboard() {
        SignalResponseDTO signal = strategy.calculateSignal(linear(200, 1000, 1));
        Map<String, Double> ind = signal.indicators();

        for (String key : List.of("support", "resistance", "vwap", "atr", "macdHistogram", "sma50")) {
            assertTrue(ind.containsKey(key), "falta el indicador " + key);
        }
        assertTrue(ind.get("support") < ind.get("resistance"));
        assertTrue(ind.get("atr") > 0);
    }
}
