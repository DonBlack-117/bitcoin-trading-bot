package com.trading.bot.dto;

import java.util.List;

public record AnalysisResponseDTO(
        // Crypto info
        String symbol,
        String name,
        int    rank,
        double priceMxn,
        double pct1h,
        double pct24h,
        double pct7d,
        double volume24h,
        double marketCap,

        // Signal
        String signal,       // COMPRAR | VENDER | MANTENER
        String emoji,
        String color,
        int    confidence,   // 0-100

        // Analysis
        List<String> reasons,

        // Scenario with user's amount
        double amountMxn,
        double unitsToBuy,

        // Expected outcomes (final MXN)
        double optimisticMxn,
        double expectedMxn,
        double riskMxn,

        // % returns for each scenario
        double optimisticPct,
        double expectedPct,
        double riskPct
) {}
