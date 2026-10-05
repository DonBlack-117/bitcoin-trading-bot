package com.trading.bot.dto;

import com.trading.bot.domain.CloseReason;
import com.trading.bot.domain.TradeStatus;
import com.trading.bot.domain.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeDTO(
        Long id,
        String symbol,
        TradeType tradeType,
        BigDecimal entryPrice,
        BigDecimal exitPrice,
        BigDecimal quantity,
        BigDecimal investedMxn,
        BigDecimal stopLoss,
        BigDecimal takeProfit,
        TradeStatus status,
        CloseReason closeReason,
        BigDecimal profitLoss,
        BigDecimal profitLossPct,
        BigDecimal currentPrice,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPct,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {}
