package com.trading.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * Reglas del bot simulado. Se pueden cambiar con propiedades bot.* o variables BOT_*.
 *
 * @param initialMxn       capital inicial del portafolio
 * @param minConfidence    confianza mínima (%) para abrir o cerrar por señal
 * @param positionFraction parte del saldo en pesos que se invierte en cada compra
 * @param minOrderMxn      saldo mínimo para abrir una operación
 * @param stopLossAtr      distancia del stop loss en múltiplos de ATR
 * @param takeProfitAtr    distancia del take profit en múltiplos de ATR
 */
@ConfigurationProperties("bot")
public record BotProperties(
        @DefaultValue("50000") BigDecimal initialMxn,
        @DefaultValue("60") int minConfidence,
        @DefaultValue("0.10") BigDecimal positionFraction,
        @DefaultValue("100") BigDecimal minOrderMxn,
        @DefaultValue("2.0") double stopLossAtr,
        @DefaultValue("3.0") double takeProfitAtr
) {}
