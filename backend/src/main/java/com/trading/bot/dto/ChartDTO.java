package com.trading.bot.dto;

import java.util.List;

/** Velas y series de indicadores para las gráficas; timestamps en segundos. */
public record ChartDTO(
        List<OhlcvCandleDTO> candles,
        List<Point> rsi,
        List<BandPoint> bollinger
) {
    public record Point(long timestamp, double value) {}

    public record BandPoint(long timestamp, double upper, double middle, double lower) {}
}
