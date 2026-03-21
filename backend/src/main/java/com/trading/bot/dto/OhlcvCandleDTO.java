package com.trading.bot.dto;

public record OhlcvCandleDTO(
        long timestamp,
        double open,
        double high,
        double low,
        double close,
        double volume
) {}
