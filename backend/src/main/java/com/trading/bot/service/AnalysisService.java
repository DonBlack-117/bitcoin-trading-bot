package com.trading.bot.service;

import com.trading.bot.dto.AnalysisRequestDTO;
import com.trading.bot.dto.AnalysisResponseDTO;
import com.trading.bot.dto.CryptoDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    private final CoinMarketCapService cmcService;

    public AnalysisService(CoinMarketCapService cmcService) {
        this.cmcService = cmcService;
    }

    public AnalysisResponseDTO analyze(AnalysisRequestDTO req) {
        String symbol    = req.symbol().toUpperCase().trim();
        double amountMxn = req.amountMxn();

        if (symbol.isBlank())  throw new IllegalArgumentException("El símbolo no puede estar vacío");
        if (amountMxn <= 0)    throw new IllegalArgumentException("El monto debe ser mayor a 0");

        // ── 1. Fetch top 100 in MXN ──────────────────────────────────────────
        List<CryptoDTO> cryptos = cmcService.getCryptoList(100, "MXN");

        CryptoDTO crypto = cryptos.stream()
                .filter(c -> c.symbol().equalsIgnoreCase(symbol))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Criptomoneda '" + symbol + "' no encontrada en el top 100"));

        double priceMxn = crypto.price();
        double pct1h    = crypto.pct1h()  != null ? crypto.pct1h()  : 0.0;
        double pct24h   = crypto.pct24h() != null ? crypto.pct24h() : 0.0;
        double pct7d    = crypto.pct7d()  != null ? crypto.pct7d()  : 0.0;
        double vol      = crypto.volume24h();
        double mc       = crypto.marketCap();

        // ── 2. Signal calculation ─────────────────────────────────────────────
        double score = 0;

        // Short-term momentum (1h) — weight 20%
        if      (pct1h >  1.5) score += 2;
        else if (pct1h >  0.3) score += 1;
        else if (pct1h < -1.5) score -= 2;
        else if (pct1h < -0.3) score -= 1;

        // Medium-term trend (24h) — weight 50%
        if      (pct24h >  6.0) score += 5;
        else if (pct24h >  3.0) score += 3;
        else if (pct24h >  1.0) score += 2;
        else if (pct24h >  0.3) score += 1;
        else if (pct24h < -6.0) score -= 5;
        else if (pct24h < -3.0) score -= 3;
        else if (pct24h < -1.0) score -= 2;
        else if (pct24h < -0.3) score -= 1;

        // Long-term trend (7d) — weight 30%
        if      (pct7d >  12.0) score += 3;
        else if (pct7d >   5.0) score += 2;
        else if (pct7d >   1.5) score += 1;
        else if (pct7d < -12.0) score -= 3;
        else if (pct7d <  -5.0) score -= 2;
        else if (pct7d <  -1.5) score -= 1;

        // Volume confirmation: high relative volume amplifies the signal
        double volRatio = mc > 0 ? vol / mc : 0;
        if (volRatio > 0.12 && pct24h > 0) score += 1.5;
        if (volRatio > 0.12 && pct24h < 0) score -= 1.5;

        // Momentum consistency: all three timeframes agree
        if (pct1h > 0 && pct24h > 0 && pct7d > 0) score += 2;
        if (pct1h < 0 && pct24h < 0 && pct7d < 0) score -= 2;

        // ── 3. Determine signal ───────────────────────────────────────────────
        String signal, emoji, color;
        int confidence;

        if (score >= 7) {
            signal = "COMPRAR"; emoji = "🟢"; color = "#22c55e";
            confidence = Math.min(92, (int)(55 + score * 3));
        } else if (score <= -7) {
            signal = "VENDER";  emoji = "🔴"; color = "#ef4444";
            confidence = Math.min(92, (int)(55 + Math.abs(score) * 3));
        } else if (score >= 3) {
            signal = "COMPRAR"; emoji = "🟡"; color = "#f59e0b";
            confidence = Math.min(68, (int)(45 + score * 3));
        } else if (score <= -3) {
            signal = "VENDER";  emoji = "🟡"; color = "#f59e0b";
            confidence = Math.min(68, (int)(45 + Math.abs(score) * 3));
        } else {
            signal = "MANTENER"; emoji = "⚪"; color = "#6b7280";
            confidence = 50;
        }

        // ── 4. Build reasoning ────────────────────────────────────────────────
        List<String> reasons = buildReasons(pct1h, pct24h, pct7d, volRatio, score, signal, crypto.name());

        // ── 5. Scenario projections ───────────────────────────────────────────
        double volatility = Math.max(Math.abs(pct24h), 1.5);

        double optimisticPct, expectedPct, riskPct;
        if ("COMPRAR".equals(signal)) {
            optimisticPct = Math.min(volatility * 2.0, 30.0);
            expectedPct   = Math.max(volatility * 0.5, 1.0);
            riskPct       = -volatility * 0.8;
        } else if ("VENDER".equals(signal)) {
            optimisticPct = volatility * 0.3;   // small upside (missed drop)
            expectedPct   = -volatility * 0.5;
            riskPct       = -Math.min(volatility * 2.0, 30.0);
        } else {
            optimisticPct = volatility * 0.6;
            expectedPct   = 0.0;
            riskPct       = -volatility * 0.6;
        }

        double optimisticMxn = amountMxn * (1 + optimisticPct / 100.0);
        double expectedMxn   = amountMxn * (1 + expectedPct   / 100.0);
        double riskMxn       = amountMxn * (1 + riskPct       / 100.0);

        double unitsToBuy = priceMxn > 0 ? amountMxn / priceMxn : 0;

        return new AnalysisResponseDTO(
                crypto.symbol(), crypto.name(), crypto.rank(),
                priceMxn, pct1h, pct24h, pct7d, vol, mc,
                signal, emoji, color, confidence,
                reasons,
                amountMxn, unitsToBuy,
                optimisticMxn, expectedMxn, riskMxn,
                optimisticPct, expectedPct, riskPct
        );
    }

    private List<String> buildReasons(double pct1h, double pct24h, double pct7d,
                                       double volRatio, double score,
                                       String signal, String name) {
        List<String> r = new ArrayList<>();

        // 1h
        if (Math.abs(pct1h) > 0.3) {
            r.add(String.format("En la última hora el precio %s un %.2f%%",
                    pct1h > 0 ? "subió" : "bajó", Math.abs(pct1h)));
        }

        // 24h
        if (Math.abs(pct24h) > 0.5) {
            r.add(String.format("En las últimas 24h %s acumuló un %s de %.2f%%",
                    name, pct24h > 0 ? "avance" : "retroceso", Math.abs(pct24h)));
        } else {
            r.add(String.format("En 24h el precio se mantuvo casi estable (%.2f%%)", pct24h));
        }

        // 7d
        if (Math.abs(pct7d) > 2.0) {
            r.add(String.format("La tendencia semanal es %s (%.2f%% en 7 días)",
                    pct7d > 0 ? "positiva" : "negativa", pct7d));
        }

        // Volume
        if (volRatio > 0.15) {
            r.add("El volumen de operaciones es alto, lo que refuerza la señal actual");
        } else if (volRatio < 0.03) {
            r.add("El volumen es bajo; la señal podría ser menos confiable");
        }

        // Consistency
        if (pct1h > 0 && pct24h > 0 && pct7d > 0) {
            r.add("Los tres marcos temporales (1h, 24h, 7d) coinciden en tendencia alcista");
        } else if (pct1h < 0 && pct24h < 0 && pct7d < 0) {
            r.add("Los tres marcos temporales (1h, 24h, 7d) coinciden en tendencia bajista");
        } else {
            r.add("Los indicadores muestran señales mixtas entre distintos períodos de tiempo");
        }

        // Overall
        if ("MANTENER".equals(signal)) {
            r.add("Con señales ambiguas, lo más prudente es esperar una confirmación antes de operar");
        }

        return r;
    }
}
