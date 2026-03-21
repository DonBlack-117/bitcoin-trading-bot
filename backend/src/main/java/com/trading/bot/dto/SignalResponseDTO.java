package com.trading.bot.dto;

import java.util.Map;

public record SignalResponseDTO(
        String signal,
        int confidence,
        String description,
        String cssClass,
        String color,
        String emoji,
        Map<String, Integer> votes,
        Map<String, Double> indicators,
        double rsi,
        double price,
        int scoreBuy,
        int scoreSell
) {}
