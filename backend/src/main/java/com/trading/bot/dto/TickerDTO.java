package com.trading.bot.dto;

public record TickerDTO(
        double last,
        double ask,
        double bid,
        double volume,
        double change24h
) {}
