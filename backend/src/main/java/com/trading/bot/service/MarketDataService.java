package com.trading.bot.service;

import com.trading.bot.client.BinanceClient;
import com.trading.bot.client.BitsoClient;
import com.trading.bot.dto.ChartDTO;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.TickerDTO;
import com.trading.bot.indicator.Indicators;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Precio de Bitso y velas de Binance convertidas a pesos.
 *
 * Las velas se convierten con el tipo de cambio implícito de ahora (precio de Bitso entre el
 * último cierre de Binance). Las velas viejas quedan con ese mismo tipo de cambio: la forma de
 * la gráfica y los indicadores no cambian, pero el nivel en pesos de hace días es aproximado.
 */
@Service
public class MarketDataService {

    private static final Duration TICKER_TTL = Duration.ofSeconds(10);
    private static final int RSI_PERIOD = 14;
    private static final int BOLLINGER_PERIOD = 20;

    private final BitsoClient bitsoClient;
    private final BinanceClient binanceClient;
    private final Clock clock;

    /** Último resultado de Bitso: el ticker o el error, con la hora en que se obtuvo. */
    private record CachedTicker(TickerDTO ticker, RuntimeException error, Instant at) {}

    private final AtomicReference<CachedTicker> cachedTicker = new AtomicReference<>();
    private final Object tickerFetchLock = new Object();

    public MarketDataService(BitsoClient bitsoClient, BinanceClient binanceClient, Clock clock) {
        this.bitsoClient = bitsoClient;
        this.binanceClient = binanceClient;
        this.clock = clock;
    }

    /**
     * Ticker con caché de 10 s: /trades, /portfolio y /chart lo piden en la misma ronda.
     * Un error también se guarda 10 s, así una caída de Bitso no deja cada petición
     * esperando sus propios reintentos. Solo un hilo consulta Bitso a la vez.
     */
    public TickerDTO getTicker() {
        CachedTicker cached = fresh(cachedTicker.get());
        if (cached == null) {
            synchronized (tickerFetchLock) {
                cached = fresh(cachedTicker.get());
                if (cached == null) {
                    cached = fetchTicker();
                    cachedTicker.set(cached);
                }
            }
        }
        if (cached.error() != null) throw cached.error();
        return cached.ticker();
    }

    private CachedTicker fresh(CachedTicker cached) {
        return cached != null && clock.instant().isBefore(cached.at().plus(TICKER_TTL)) ? cached : null;
    }

    private CachedTicker fetchTicker() {
        try {
            return new CachedTicker(bitsoClient.fetchTicker(), null, clock.instant());
        } catch (RuntimeException e) {
            return new CachedTicker(null, e, clock.instant());
        }
    }

    public double currentPrice() {
        return getTicker().last();
    }

    /** Velas de 1 h en pesos, de la más antigua a la más reciente. */
    public List<OhlcvCandleDTO> getCandlesMxn(int limit) {
        List<OhlcvCandleDTO> usd = binanceClient.fetchHourlyCandlesUsd(limit);
        double usdToMxn = currentPrice() / usd.get(usd.size() - 1).close();
        return usd.stream()
                .map(c -> new OhlcvCandleDTO(c.timestamp(),
                        c.open() * usdToMxn, c.high() * usdToMxn,
                        c.low() * usdToMxn, c.close() * usdToMxn,
                        c.volume()))
                .toList();
    }

    /** Velas con RSI y bandas de Bollinger ya calculadas, con los mismos métodos que la estrategia. */
    public ChartDTO getChart(int limit) {
        List<OhlcvCandleDTO> candles = getCandlesMxn(limit);
        List<Double> closes = candles.stream().map(OhlcvCandleDTO::close).toList();

        List<Double> rsi = Indicators.rsi(closes, RSI_PERIOD);
        List<ChartDTO.Point> rsiSeries = new ArrayList<>(rsi.size());
        for (int i = 0; i < rsi.size(); i++) {
            rsiSeries.add(new ChartDTO.Point(candles.get(i + RSI_PERIOD).timestamp(), rsi.get(i)));
        }

        List<Indicators.Band> bands = Indicators.bollinger(closes, BOLLINGER_PERIOD);
        List<ChartDTO.BandPoint> bandSeries = new ArrayList<>(bands.size());
        for (int i = 0; i < bands.size(); i++) {
            Indicators.Band b = bands.get(i);
            long ts = candles.get(i + BOLLINGER_PERIOD - 1).timestamp();
            bandSeries.add(new ChartDTO.BandPoint(ts, b.upper(), b.middle(), b.lower()));
        }
        return new ChartDTO(candles, rsiSeries, bandSeries);
    }
}
