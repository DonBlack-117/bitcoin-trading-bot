package com.trading.bot.dto;

public record PortfolioDTO(
        Double initialCapital,
        Double mxnBalance,
        Double btcBalance,
        Double btcPrice,
        Double totalValueMxn,
        Double unrealizedPnl,
        Double totalReturn,
        Double totalReturnPct,
        Integer totalTrades,
        Integer winningTrades,
        Double winRate
) {}
