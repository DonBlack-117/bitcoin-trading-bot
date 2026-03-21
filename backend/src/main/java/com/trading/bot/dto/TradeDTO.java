package com.trading.bot.dto;

import java.time.LocalDateTime;

public record TradeDTO(
        Long id,
        String symbol,
        String tradeType,
        Double entryPrice,
        Double exitPrice,
        Double quantity,
        Double investedMxn,
        Double stopLoss,
        Double takeProfit,
        String status,
        String closeReason,
        Double profitLoss,
        Double profitLossPct,
        Double currentPrice,
        Double unrealizedPnl,
        Double unrealizedPnlPct,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {}
