package com.trading.bot.support;

import com.trading.bot.config.BotProperties;
import com.trading.bot.domain.SignalType;
import com.trading.bot.dto.SignalResponseDTO;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

/** Datos comunes de las pruebas. */
public final class TestData {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-04T18:00:00Z"), ZoneId.of("America/Mexico_City"));

    private TestData() {}

    public static BotProperties properties() {
        return new BotProperties(new BigDecimal("50000"), 60, new BigDecimal("0.10"),
                new BigDecimal("100"), 2.0, 3.0);
    }

    public static SignalResponseDTO signal(SignalType type, int confidence, boolean actionable,
                                           double price, double atr) {
        return new SignalResponseDTO(type, confidence, actionable, "prueba",
                Map.of("macd", 0), Map.of("atr", atr), 50.0, price, 0, 0, CLOCK.instant());
    }
}
