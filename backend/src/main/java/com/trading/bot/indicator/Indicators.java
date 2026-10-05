package com.trading.bot.indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Indicadores técnicos sobre series de precios, de la más antigua a la más reciente.
 * Los usan la estrategia del bot y las series de las gráficas, así ambos dan los mismos números.
 */
public final class Indicators {

    private Indicators() {}

    /** Bandas de Bollinger en un punto: media ± 2 desviaciones estándar. */
    public record Band(double upper, double middle, double lower) {}

    /** Media móvil simple. El valor i corresponde al precio i + period - 1. */
    public static List<Double> sma(List<Double> prices, int period) {
        List<Double> result = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < prices.size(); i++) {
            sum += prices.get(i);
            if (i >= period) sum -= prices.get(i - period);
            if (i >= period - 1) result.add(sum / period);
        }
        return result;
    }

    /** Media móvil exponencial sembrada con la SMA del primer periodo. */
    public static List<Double> ema(List<Double> prices, int period) {
        List<Double> result = new ArrayList<>();
        if (prices.size() < period) return result;
        double multiplier = 2.0 / (period + 1);
        double sum = 0;
        for (int i = 0; i < period; i++) sum += prices.get(i);
        double prevEma = sum / period;
        result.add(prevEma);
        for (int i = period; i < prices.size(); i++) {
            prevEma = (prices.get(i) - prevEma) * multiplier + prevEma;
            result.add(prevEma);
        }
        return result;
    }

    /** RSI de Wilder. El valor i corresponde al precio i + period. */
    public static List<Double> rsi(List<Double> prices, int period) {
        List<Double> result = new ArrayList<>();
        if (prices.size() <= period) return result;
        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) avgGain += change; else avgLoss -= change;
        }
        avgGain /= period;
        avgLoss /= period;
        result.add(rsiValue(avgGain, avgLoss));
        for (int i = period + 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            avgGain = (avgGain * (period - 1) + Math.max(change, 0)) / period;
            avgLoss = (avgLoss * (period - 1) + Math.max(-change, 0)) / period;
            result.add(rsiValue(avgGain, avgLoss));
        }
        return result;
    }

    private static double rsiValue(double avgGain, double avgLoss) {
        return avgLoss == 0 ? 100.0 : 100.0 - (100.0 / (1.0 + avgGain / avgLoss));
    }

    /** Bandas de Bollinger por punto. El valor i corresponde al precio i + period - 1. */
    public static List<Band> bollinger(List<Double> prices, int period) {
        List<Band> result = new ArrayList<>();
        for (int end = period; end <= prices.size(); end++) {
            List<Double> window = prices.subList(end - period, end);
            double mean = window.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = window.stream().mapToDouble(p -> (p - mean) * (p - mean)).average().orElse(0);
            double std = Math.sqrt(variance);
            result.add(new Band(mean + 2 * std, mean, mean - 2 * std));
        }
        return result;
    }

    /** ATR de Wilder; con pocas velas usa el 1 % del último cierre. */
    public static double atr(List<Double> highs, List<Double> lows, List<Double> closes, int period) {
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

    /** On-Balance Volume acumulado, uno por vela. */
    public static List<Double> obv(List<Double> closes, List<Double> volumes) {
        List<Double> obv = new ArrayList<>(closes.size());
        obv.add(volumes.get(0));
        for (int i = 1; i < closes.size(); i++) {
            double prev = obv.get(i - 1);
            double direction = Math.signum(closes.get(i) - closes.get(i - 1));
            obv.add(prev + direction * volumes.get(i));
        }
        return obv;
    }

    /** VWAP de las últimas `period` velas con el precio típico (máximo + mínimo + cierre) / 3. */
    public static double vwap(List<Double> highs, List<Double> lows, List<Double> closes,
                              List<Double> volumes, int period) {
        int n = closes.size();
        double tpv = 0, vol = 0;
        for (int i = Math.max(0, n - period); i < n; i++) {
            double tp = (highs.get(i) + lows.get(i) + closes.get(i)) / 3.0;
            tpv += tp * volumes.get(i);
            vol += volumes.get(i);
        }
        return vol > 0 ? tpv / vol : closes.get(n - 1);
    }
}
