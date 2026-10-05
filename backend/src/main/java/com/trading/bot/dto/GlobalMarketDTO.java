package com.trading.bot.dto;

import java.util.List;

public record GlobalMarketDTO(
        double totalMarketCap,
        double totalVolume24h,
        double btcDominance,
        double ethDominance,
        long activeCryptos,
        long activeExchanges,
        double marketCapChange24h,
        List<CryptoDTO> topCryptos,
        int fearGreedScore,
        String fearGreedLabel
) {}
