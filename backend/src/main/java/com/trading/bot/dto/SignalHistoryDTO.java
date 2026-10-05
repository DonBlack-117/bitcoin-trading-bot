package com.trading.bot.dto;

import com.trading.bot.domain.SignalType;
import com.trading.bot.model.SignalHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SignalHistoryDTO(
        Long id,
        LocalDateTime timestamp,
        SignalType signal,
        BigDecimal price,
        int confidence,
        Integer scoreBuy,
        Integer scoreSell
) {
    public static SignalHistoryDTO from(SignalHistory s) {
        return new SignalHistoryDTO(s.getId(), s.getTimestamp(), s.getSenal(), s.getPrecio(),
                s.getConfianza(), s.getScoreBuy(), s.getScoreSell());
    }
}
