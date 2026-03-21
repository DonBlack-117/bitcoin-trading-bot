package com.trading.bot.service;

import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.SignalResponseDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StrategyService {

    private record MacdResult(
            double macdLast, double macdPrev,
            double signalLast, double signalPrev,
            double histLast, double histPrev) {}

    public SignalResponseDTO calculateSignal(List<OhlcvCandleDTO> candles) {
        if (candles == null || candles.size() < 30) {
            throw new RuntimeException("Not enough candle data for strategy calculation");
        }

        int n = candles.size();

        List<Double> closes  = candles.stream().map(OhlcvCandleDTO::close).collect(Collectors.toList());
        List<Double> highs   = candles.stream().map(OhlcvCandleDTO::high).collect(Collectors.toList());
        List<Double> lows    = candles.stream().map(OhlcvCandleDTO::low).collect(Collectors.toList());
        List<Double> volumes = candles.stream().map(OhlcvCandleDTO::volume).collect(Collectors.toList());

        double currentClose = closes.get(n - 1);
        double prevClose    = closes.get(n - 2);

        // ── Moving averages ──────────────────────────────────────────────────
        List<Double> sma20  = sma(closes, 20);
        List<Double> sma50  = sma(closes, 50);
        List<Double> sma200 = sma(closes, 200);
        List<Double> ema12  = ema(closes, 12);
        List<Double> ema26  = ema(closes, 26);

        double curEma12  = last(ema12,  currentClose);
        double curEma26  = last(ema26,  currentClose);
        double prevEma12 = prev(ema12,  curEma12);
        double prevEma26 = prev(ema26,  curEma26);
        double curSma50  = last(sma50,  currentClose);
        double curSma200 = last(sma200, currentClose);

        // ── RSI ──────────────────────────────────────────────────────────────
        List<Double> rsiValues = rsi(closes, 14);
        double currentRsi = rsiValues.isEmpty() ? 50.0 : rsiValues.get(rsiValues.size() - 1);

        // ── MACD ─────────────────────────────────────────────────────────────
        MacdResult macd = calculateMacd(closes);

        // ── Bollinger Bands ──────────────────────────────────────────────────
        double[] bb = bollinger(closes, 20);   // [upper, middle, lower]

        // ── ATR ──────────────────────────────────────────────────────────────
        double currentAtr = calculateAtr(highs, lows, closes, 14);

        // ── OBV ──────────────────────────────────────────────────────────────
        List<Double> obvSeries = calculateObvSeries(closes, volumes);
        double curObv  = obvSeries.get(n - 1);
        double prevObv = obvSeries.get(n - 2);

        // ── VWAP (last 24 candles) ────────────────────────────────────────────
        double vwap = calculateVwap(highs, lows, closes, volumes, 24);

        // ── Support / Resistance (last 50 candles) ───────────────────────────
        int    srWindow     = Math.min(50, n);
        double support      = lows.subList(n - srWindow, n).stream().mapToDouble(d -> d).min().orElse(currentClose * 0.95);
        double resistance   = highs.subList(n - srWindow, n).stream().mapToDouble(d -> d).max().orElse(currentClose * 1.05);

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
        if (currentRsi < 30 && currentClose <= bb[2]) {
            scoreRsiBb = 3;        // sobrevendido extremo
        } else if (currentRsi < 40 && currentClose <= bb[2]) {
            scoreRsiBb = 2;        // zona de sobreventa
        } else if (currentRsi > 70 && currentClose >= bb[0]) {
            scoreRsiBb = -3;       // sobrecomprado extremo
        } else if (currentRsi > 60 && currentClose >= bb[0]) {
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
        votes.put("emaCruce",          scoreEma);
        votes.put("rsiBollinger",      scoreRsiBb);
        votes.put("macd",              scoreMacd);
        votes.put("volumenVwap",       scoreVol);
        votes.put("soporteResistencia", scoreSR);

        int scoreBuy  = votes.values().stream().filter(v -> v > 0).mapToInt(Integer::intValue).sum();
        int scoreSell = votes.values().stream().filter(v -> v < 0).mapToInt(v -> Math.abs(v)).sum();

        // ── Signal decision ───────────────────────────────────────────────────
        String signal;
        int    confidence;
        String emoji, cssClass, color;

        if (scoreBuy >= 6 && scoreBuy > scoreSell) {
            signal = "COMPRAR"; emoji = "🟢"; cssClass = "signal-buy";  color = "#22c55e";
            confidence = Math.min(95, 50 + (scoreBuy - scoreSell) * 3);
        } else if (scoreSell >= 6 && scoreSell > scoreBuy) {
            signal = "VENDER";  emoji = "🔴"; cssClass = "signal-sell"; color = "#ef4444";
            confidence = Math.min(95, 50 + (scoreSell - scoreBuy) * 3);
        } else if (scoreBuy > scoreSell) {
            signal = "COMPRAR"; emoji = "🟡"; cssClass = "signal-hold"; color = "#f59e0b";
            confidence = Math.min(68, 45 + scoreBuy * 2);
        } else if (scoreSell > scoreBuy) {
            signal = "VENDER";  emoji = "🟡"; cssClass = "signal-hold"; color = "#f59e0b";
            confidence = Math.min(68, 45 + scoreSell * 2);
        } else {
            signal = "MANTENER"; emoji = "🟡"; cssClass = "signal-hold"; color = "#f59e0b";
            confidence = 50;
        }

        String description = buildDescription(signal, confidence, currentRsi, scoreBuy, scoreSell);

        // ── Indicators snapshot ───────────────────────────────────────────────
        Map<String, Double> indicators = new LinkedHashMap<>();
        indicators.put("rsi",              currentRsi);
        indicators.put("macdLine",         macd.macdLast());
        indicators.put("macdSignal",       macd.signalLast());
        indicators.put("macdHistogram",    macd.histLast());
        indicators.put("bollingerUpper",   bb[0]);
        indicators.put("bollingerMiddle",  bb[1]);
        indicators.put("bollingerLower",   bb[2]);
        indicators.put("atr",              currentAtr);
        indicators.put("vwap",             vwap);
        indicators.put("support",          support);
        indicators.put("resistance",       resistance);
        indicators.put("sma20",            last(sma20,  currentClose));
        indicators.put("sma50",            curSma50);
        indicators.put("ema12",            curEma12);
        indicators.put("ema26",            curEma26);

        return new SignalResponseDTO(signal, confidence, description, cssClass, color, emoji,
                votes, indicators, currentRsi, currentClose, scoreBuy, scoreSell);
    }

    // ── Decision description ──────────────────────────────────────────────────
    private String buildDescription(String signal, int confidence, double rsi,
                                    int scoreBuy, int scoreSell) {
        return switch (signal) {
            case "COMPRAR" -> String.format(
                    "Señal de compra con %d%% confianza. RSI: %.1f | Puntaje alcista: %d vs bajista: %d",
                    confidence, rsi, scoreBuy, scoreSell);
            case "VENDER" -> String.format(
                    "Señal de venta con %d%% confianza. RSI: %.1f | Puntaje bajista: %d vs alcista: %d",
                    confidence, rsi, scoreSell, scoreBuy);
            default -> String.format(
                    "Mercado sin dirección clara. RSI: %.1f | Puntaje: alcista %d / bajista %d. Esperar confirmación.",
                    rsi, scoreBuy, scoreSell);
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INDICATOR CALCULATIONS
    // ══════════════════════════════════════════════════════════════════════════

    private List<Double> sma(List<Double> prices, int period) {
        List<Double> result = new ArrayList<>();
        for (int i = period - 1; i < prices.size(); i++) {
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) sum += prices.get(j);
            result.add(sum / period);
        }
        return result;
    }

    private List<Double> ema(List<Double> prices, int period) {
        if (prices.size() < period) return new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        double sum = 0;
        for (int i = 0; i < period; i++) sum += prices.get(i);
        double prevEma = sum / period;
        List<Double> result = new ArrayList<>();
        result.add(prevEma);
        for (int i = period; i < prices.size(); i++) {
            double cur = (prices.get(i) - prevEma) * multiplier + prevEma;
            result.add(cur);
            prevEma = cur;
        }
        return result;
    }

    private List<Double> rsi(List<Double> prices, int period) {
        List<Double> result = new ArrayList<>();
        if (prices.size() <= period) return result;
        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) avgGain += change; else avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;
        result.add(avgLoss == 0 ? 100.0 : 100.0 - (100.0 / (1.0 + avgGain / avgLoss)));
        for (int i = period + 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            avgGain = (avgGain * (period - 1) + Math.max(change, 0)) / period;
            avgLoss = (avgLoss * (period - 1) + Math.abs(Math.min(change, 0))) / period;
            result.add(avgLoss == 0 ? 100.0 : 100.0 - (100.0 / (1.0 + avgGain / avgLoss)));
        }
        return result;
    }

    private MacdResult calculateMacd(List<Double> prices) {
        List<Double> e12 = ema(prices, 12);
        List<Double> e26 = ema(prices, 26);
        if (e12.size() < 2 || e26.isEmpty()) return new MacdResult(0, 0, 0, 0, 0, 0);

        int diff = e12.size() - e26.size();
        List<Double> macdLine = new ArrayList<>();
        for (int i = 0; i < e26.size(); i++) macdLine.add(e12.get(i + diff) - e26.get(i));

        List<Double> signalLine = ema(macdLine, 9);
        double ml = macdLine.get(macdLine.size() - 1);
        double mp = macdLine.size() >= 2 ? macdLine.get(macdLine.size() - 2) : ml;
        double sl = signalLine.isEmpty() ? 0 : signalLine.get(signalLine.size() - 1);
        double sp = signalLine.size() >= 2 ? signalLine.get(signalLine.size() - 2) : sl;
        return new MacdResult(ml, mp, sl, sp, ml - sl, mp - sp);
    }

    private double[] bollinger(List<Double> prices, int period) {
        if (prices.size() < period) return new double[]{0, 0, 0};
        List<Double> window = prices.subList(prices.size() - period, prices.size());
        double mean = window.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double std  = Math.sqrt(window.stream().mapToDouble(p -> Math.pow(p - mean, 2)).average().orElse(0));
        return new double[]{mean + 2 * std, mean, mean - 2 * std};
    }

    private double calculateAtr(List<Double> highs, List<Double> lows, List<Double> closes, int period) {
        int n = closes.size();
        if (n < period + 1) return closes.get(n - 1) * 0.01;
        double[] tr = new double[n];
        tr[0] = highs.get(0) - lows.get(0);
        for (int i = 1; i < n; i++) {
            double hl = highs.get(i) - lows.get(i);
            double hc = Math.abs(highs.get(i) - closes.get(i - 1));
            double lc = Math.abs(lows.get(i) - closes.get(i - 1));
            tr[i] = Math.max(hl, Math.max(hc, lc));
        }
        double atr = 0;
        for (int i = 0; i < period; i++) atr += tr[i];
        atr /= period;
        for (int i = period; i < n; i++) atr = (atr * (period - 1) + tr[i]) / period;
        return atr;
    }

    private List<Double> calculateObvSeries(List<Double> closes, List<Double> volumes) {
        List<Double> obv = new ArrayList<>(closes.size());
        obv.add(volumes.get(0));
        for (int i = 1; i < closes.size(); i++) {
            double prev = obv.get(i - 1);
            if (closes.get(i) > closes.get(i - 1))       obv.add(prev + volumes.get(i));
            else if (closes.get(i) < closes.get(i - 1))  obv.add(prev - volumes.get(i));
            else                                           obv.add(prev);
        }
        return obv;
    }

    private double calculateVwap(List<Double> highs, List<Double> lows, List<Double> closes,
                                  List<Double> volumes, int period) {
        int n = closes.size();
        int start = Math.max(0, n - period);
        double tpv = 0, vol = 0;
        for (int i = start; i < n; i++) {
            double tp = (highs.get(i) + lows.get(i) + closes.get(i)) / 3.0;
            tpv += tp * volumes.get(i);
            vol += volumes.get(i);
        }
        return vol > 0 ? tpv / vol : closes.get(n - 1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private double last(List<Double> list, double fallback) {
        return list.isEmpty() ? fallback : list.get(list.size() - 1);
    }

    private double prev(List<Double> list, double fallback) {
        return list.size() >= 2 ? list.get(list.size() - 2) : fallback;
    }
}
