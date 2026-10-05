package com.trading.bot.service;

import com.trading.bot.config.BotProperties;
import com.trading.bot.domain.SignalType;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.indicator.Indicators;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Combina cinco estrategias por votos (de −3 a +3 cada una) en una sola señal. */
@Service
public class StrategyService {

    /** Velas mínimas para que MACD (26 + 9) tenga sentido. */
    public static final int MIN_CANDLES = 30;

    /** Puntaje desde el que una señal es fuerte. */
    private static final int STRONG_SCORE = 6;

    private final BotProperties properties;
    private final Clock clock;

    public StrategyService(BotProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    private record MacdResult(
            double macdLast, double macdPrev,
            double signalLast, double signalPrev,
            double histLast, double histPrev) {}

    public SignalResponseDTO calculateSignal(List<OhlcvCandleDTO> candles) {
        if (candles == null || candles.size() < MIN_CANDLES) {
            throw new IllegalArgumentException("Se necesitan al menos " + MIN_CANDLES + " velas para calcular la señal");
        }

        int n = candles.size();

        List<Double> closes  = candles.stream().map(OhlcvCandleDTO::close).toList();
        List<Double> highs   = candles.stream().map(OhlcvCandleDTO::high).toList();
        List<Double> lows    = candles.stream().map(OhlcvCandleDTO::low).toList();
        List<Double> volumes = candles.stream().map(OhlcvCandleDTO::volume).toList();

        double currentClose = closes.get(n - 1);
        double prevClose    = closes.get(n - 2);

        // ── Moving averages ──────────────────────────────────────────────────
        List<Double> sma20  = Indicators.sma(closes, 20);
        List<Double> sma50  = Indicators.sma(closes, 50);
        List<Double> sma200 = Indicators.sma(closes, 200);
        List<Double> ema12  = Indicators.ema(closes, 12);
        List<Double> ema26  = Indicators.ema(closes, 26);

        double curEma12  = last(ema12,  currentClose);
        double curEma26  = last(ema26,  currentClose);
        double prevEma12 = prev(ema12,  curEma12);
        double prevEma26 = prev(ema26,  curEma26);
        double curSma50  = last(sma50,  currentClose);
        double curSma200 = last(sma200, currentClose);

        // ── RSI ──────────────────────────────────────────────────────────────
        double currentRsi = last(Indicators.rsi(closes, 14), 50.0);

        // ── MACD ─────────────────────────────────────────────────────────────
        MacdResult macd = calculateMacd(closes);

        // ── Bollinger Bands ──────────────────────────────────────────────────
        List<Indicators.Band> bands = Indicators.bollinger(closes, 20);
        Indicators.Band bb = bands.isEmpty() ? new Indicators.Band(0, 0, 0) : bands.get(bands.size() - 1);

        // ── ATR, OBV y VWAP (últimas 24 velas) ───────────────────────────────
        double currentAtr = Indicators.atr(highs, lows, closes, 14);
        List<Double> obvSeries = Indicators.obv(closes, volumes);
        double curObv  = obvSeries.get(n - 1);
        double prevObv = obvSeries.get(n - 2);
        double vwap = Indicators.vwap(highs, lows, closes, volumes, 24);

        // ── Support / Resistance (last 50 candles) ───────────────────────────
        int    srWindow   = Math.min(50, n);
        double support    = lows.subList(n - srWindow, n).stream().mapToDouble(d -> d).min().orElse(currentClose * 0.95);
        double resistance = highs.subList(n - srWindow, n).stream().mapToDouble(d -> d).max().orElse(currentClose * 1.05);

        // ════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 1 – EMA Crossover + tendencia SMA 50/200
        // ════════════════════════════════════════════════════════════════════
        int scoreEma = 0;
        if (curEma12 > curEma26 && prevEma12 <= prevEma26) {
            scoreEma = 2;          // cruce alcista EMA
        } else if (curEma12 < curEma26 && prevEma12 >= prevEma26) {
            scoreEma = -2;         // cruce bajista EMA
        } else if (!sma50.isEmpty() && !sma200.isEmpty()
                   && currentClose > curSma50 && curSma50 > curSma200) {
            scoreEma = 1;          // tendencia alcista confirmada
        } else if (!sma50.isEmpty() && !sma200.isEmpty()
                   && currentClose < curSma50 && curSma50 < curSma200) {
            scoreEma = -1;         // tendencia bajista confirmada
        }

        // ════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 2 – RSI + Bandas de Bollinger
        // ════════════════════════════════════════════════════════════════════
        int scoreRsiBb = 0;
        if (currentRsi < 30 && currentClose <= bb.lower()) {
            scoreRsiBb = 3;        // sobrevendido extremo
        } else if (currentRsi < 40 && currentClose <= bb.lower()) {
            scoreRsiBb = 2;        // zona de sobreventa
        } else if (currentRsi > 70 && currentClose >= bb.upper()) {
            scoreRsiBb = -3;       // sobrecomprado extremo
        } else if (currentRsi > 60 && currentClose >= bb.upper()) {
            scoreRsiBb = -2;       // zona de sobrecompra
        }

        // ════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 3 – MACD
        // ════════════════════════════════════════════════════════════════════
        int scoreMacd = 0;
        if (macd.macdLast() > macd.signalLast() && macd.macdPrev() <= macd.signalPrev()) {
            scoreMacd = 3;         // cruce alcista MACD
        } else if (macd.histLast() > 0 && macd.histLast() > macd.histPrev()) {
            scoreMacd = 2;         // momentum alcista creciente
        } else if (macd.macdLast() < macd.signalLast() && macd.macdPrev() >= macd.signalPrev()) {
            scoreMacd = -3;        // cruce bajista MACD
        } else if (macd.histLast() < 0 && macd.histLast() < macd.histPrev()) {
            scoreMacd = -2;        // momentum bajista creciente
        }

        // ════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 4 – Volumen (OBV) + VWAP
        // ════════════════════════════════════════════════════════════════════
        int scoreVol = 0;
        if (currentClose > prevClose && curObv > prevObv) {
            scoreVol += 1;         // subida con volumen
        } else if (currentClose < prevClose && curObv < prevObv) {
            scoreVol -= 1;         // bajada con volumen
        }
        if (currentClose > vwap) {
            scoreVol += 1;         // precio sobre VWAP
        } else if (currentClose < vwap) {
            scoreVol -= 1;         // precio bajo VWAP
        }

        // ════════════════════════════════════════════════════════════════════
        // ESTRATEGIA 5 – Soporte y Resistencia
        // ════════════════════════════════════════════════════════════════════
        int scoreSR = 0;
        if (currentClose <= support * 1.02) {
            scoreSR = 3;           // precio cerca del soporte
        } else if (currentClose >= resistance * 0.98) {
            scoreSR = -3;          // precio cerca de la resistencia
        }

        // ── Tally scores ─────────────────────────────────────────────────────
        Map<String, Integer> votes = new LinkedHashMap<>();
        votes.put("emaCruce",           scoreEma);
        votes.put("rsiBollinger",       scoreRsiBb);
        votes.put("macd",               scoreMacd);
        votes.put("volumenVwap",        scoreVol);
        votes.put("soporteResistencia", scoreSR);

        int scoreBuy  = votes.values().stream().filter(v -> v > 0).mapToInt(Integer::intValue).sum();
        int scoreSell = votes.values().stream().filter(v -> v < 0).mapToInt(Math::abs).sum();

        // ── Signal decision ───────────────────────────────────────────────────
        SignalType signal;
        int confidence;
        if (scoreBuy >= STRONG_SCORE && scoreBuy > scoreSell) {
            signal = SignalType.COMPRAR;
            confidence = Math.min(95, 50 + (scoreBuy - scoreSell) * 3);
        } else if (scoreSell >= STRONG_SCORE && scoreSell > scoreBuy) {
            signal = SignalType.VENDER;
            confidence = Math.min(95, 50 + (scoreSell - scoreBuy) * 3);
        } else if (scoreBuy > scoreSell) {
            signal = SignalType.COMPRAR;
            confidence = Math.min(68, 45 + scoreBuy * 2);
        } else if (scoreSell > scoreBuy) {
            signal = SignalType.VENDER;
            confidence = Math.min(68, 45 + scoreSell * 2);
        } else {
            signal = SignalType.MANTENER;
            confidence = 50;
        }
        boolean actionable = signal != SignalType.MANTENER && confidence >= properties.minConfidence();

        String description = buildDescription(signal, confidence, currentRsi, scoreBuy, scoreSell);

        // ── Indicators snapshot ───────────────────────────────────────────────
        Map<String, Double> indicators = new LinkedHashMap<>();
        indicators.put("rsi",              currentRsi);
        indicators.put("macdLine",         macd.macdLast());
        indicators.put("macdSignal",       macd.signalLast());
        indicators.put("macdHistogram",    macd.histLast());
        indicators.put("bollingerUpper",   bb.upper());
        indicators.put("bollingerMiddle",  bb.middle());
        indicators.put("bollingerLower",   bb.lower());
        indicators.put("atr",              currentAtr);
        indicators.put("vwap",             vwap);
        indicators.put("support",          support);
        indicators.put("resistance",       resistance);
        indicators.put("sma20",            last(sma20,  currentClose));
        indicators.put("sma50",            curSma50);
        indicators.put("ema12",            curEma12);
        indicators.put("ema26",            curEma26);

        return new SignalResponseDTO(signal, confidence, actionable, description,
                votes, indicators, currentRsi, currentClose, scoreBuy, scoreSell, Instant.now(clock));
    }

    // ── Decision description ──────────────────────────────────────────────────
    private String buildDescription(SignalType signal, int confidence, double rsi,
                                    int scoreBuy, int scoreSell) {
        return switch (signal) {
            case COMPRAR -> String.format(
                    "Señal de compra con %d%% confianza. RSI: %.1f | Puntaje alcista: %d vs bajista: %d",
                    confidence, rsi, scoreBuy, scoreSell);
            case VENDER -> String.format(
                    "Señal de venta con %d%% confianza. RSI: %.1f | Puntaje bajista: %d vs alcista: %d",
                    confidence, rsi, scoreSell, scoreBuy);
            case MANTENER -> String.format(
                    "Mercado sin dirección clara. RSI: %.1f | Puntaje: alcista %d / bajista %d. Esperar confirmación.",
                    rsi, scoreBuy, scoreSell);
        };
    }

    private MacdResult calculateMacd(List<Double> prices) {
        List<Double> e12 = Indicators.ema(prices, 12);
        List<Double> e26 = Indicators.ema(prices, 26);
        if (e12.size() < 2 || e26.isEmpty()) return new MacdResult(0, 0, 0, 0, 0, 0);

        // e12 empieza 14 velas antes que e26: se alinean por el final
        int diff = e12.size() - e26.size();
        List<Double> macdLine = new java.util.ArrayList<>(e26.size());
        for (int i = 0; i < e26.size(); i++) macdLine.add(e12.get(i + diff) - e26.get(i));

        List<Double> signalLine = Indicators.ema(macdLine, 9);
        double ml = last(macdLine, 0);
        double mp = prev(macdLine, ml);
        double sl = last(signalLine, 0);
        double sp = prev(signalLine, sl);
        return new MacdResult(ml, mp, sl, sp, ml - sl, mp - sp);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static double last(List<Double> list, double fallback) {
        return list.isEmpty() ? fallback : list.get(list.size() - 1);
    }

    private static double prev(List<Double> list, double fallback) {
        return list.size() >= 2 ? list.get(list.size() - 2) : fallback;
    }
}
