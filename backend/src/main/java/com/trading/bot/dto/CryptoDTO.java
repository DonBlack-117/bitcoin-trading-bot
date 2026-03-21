package com.trading.bot.dto;

public record CryptoDTO(
        int rank,
        String symbol,
        String name,
        double price,
        double marketCap,
        double volume24h,
        Double pct1h,
        Double pct24h,
        Double pct7d
) {}
