package com.trading.bot.dto;

import com.trading.bot.domain.SignalType;

import java.time.Instant;
import java.util.Map;

/**
 * Señal calculada por la estrategia.
 *
 * @param actionable   true si el bot opera con ella (COMPRAR o VENDER con la confianza mínima)
 * @param calculatedAt cuándo se calculó; sirve para saber si la señal está vieja
 */
public record SignalResponseDTO(
        SignalType signal,
        int confidence,
        boolean actionable,
        String description,
        Map<String, Integer> votes,
        Map<String, Double> indicators,
        double rsi,
        double price,
        int scoreBuy,
        int scoreSell,
        Instant calculatedAt
) {}
