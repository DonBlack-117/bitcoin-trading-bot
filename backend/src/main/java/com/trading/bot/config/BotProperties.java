package com.trading.bot.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Reglas del bot simulado. Se pueden cambiar con propiedades bot.* o variables BOT_*.
 * Los límites evitan configuraciones imposibles (división entre cero, saldos negativos, stops absurdos).
 *
 * @param initialMxn       capital inicial del portafolio
 * @param minConfidence    confianza mínima (%) para abrir o cerrar por señal
 * @param positionFraction parte del saldo en pesos que se invierte en cada compra (0 a 1)
 * @param minOrderMxn      saldo mínimo para abrir una operación
 * @param stopLossAtr      distancia del stop loss en múltiplos de ATR
 * @param takeProfitAtr    distancia del take profit en múltiplos de ATR
 * @param cooldownMinutes  minutos de espera tras cerrar por stop loss o take profit antes de reabrir
 */
@Validated
@ConfigurationProperties("bot")
public record BotProperties(
        @DefaultValue("50000") @DecimalMin("1") BigDecimal initialMxn,
        @DefaultValue("60") @Min(1) @Max(100) int minConfidence,
        @DefaultValue("0.10") @DecimalMin(
                value = "0", inclusive = false) @DecimalMax("1") BigDecimal positionFraction,
        @DefaultValue("100") @Positive BigDecimal minOrderMxn,
        @DefaultValue("2.0") @Positive double stopLossAtr,
        @DefaultValue("3.0") @Positive double takeProfitAtr,
        @DefaultValue("15") @PositiveOrZero int cooldownMinutes
) {}
