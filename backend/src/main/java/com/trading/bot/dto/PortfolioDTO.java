package com.trading.bot.dto;

import java.math.BigDecimal;

/**
 * Estado del portafolio simulado.
 *
 * @param totalValueMxn pesos + BTC al precio actual (la operación abierta ya está en el BTC)
 * @param unrealizedPnl ganancia o pérdida de la operación abierta, solo informativa
 */
public record PortfolioDTO(
        BigDecimal initialCapital,
        BigDecimal mxnBalance,
        BigDecimal btcBalance,
        BigDecimal btcPrice,
        BigDecimal totalValueMxn,
        BigDecimal unrealizedPnl,
        BigDecimal totalReturn,
        BigDecimal totalReturnPct,
        int totalTrades,
        int winningTrades,
        BigDecimal winRate
) {}
