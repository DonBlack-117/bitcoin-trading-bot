package com.trading.bot.service;

import com.trading.bot.domain.Money;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.exception.SignalNotReadyException;
import com.trading.bot.exception.SignalStaleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recalcula la señal en segundo plano, la guarda y procesa las operaciones simuladas.
 * GET /api/signal solo lee la última señal: una petición nunca abre ni cierra operaciones.
 */
@Component
public class SignalScheduler {
    private static final Logger log = LoggerFactory.getLogger(SignalScheduler.class);

    /** 200 velas de 1 h: lo que necesita la SMA 200. */
    static final int CANDLES = 200;

    private final MarketDataService marketDataService;
    private final StrategyService strategyService;
    private final SignalHistoryService signalHistoryService;
    private final TradeService tradeService;
    private final PortfolioService portfolioService;

    private final AtomicReference<SignalResponseDTO> latest = new AtomicReference<>();
    /** Momento de la última señal calculada con éxito, para detectar que el cálculo se quedó atascado. */
    private final AtomicReference<Instant> lastSuccessAt = new AtomicReference<>();

    private final long intervalMs;

    public SignalScheduler(MarketDataService marketDataService,
            StrategyService strategyService,
            SignalHistoryService signalHistoryService,
            TradeService tradeService,
            PortfolioService portfolioService,
            @Value("${bot.signal.interval-ms:30000}") long intervalMs) {
        this.marketDataService = marketDataService;
        this.strategyService = strategyService;
        this.signalHistoryService = signalHistoryService;
        this.tradeService = tradeService;
        this.portfolioService = portfolioService;
        this.intervalMs = intervalMs;
    }

    @Scheduled(fixedDelayString = "${bot.signal.interval-ms:30000}")
    public void scheduledRun() {
        try {
            refresh();
        } catch (Exception e) {
            log.error("No se pudo actualizar la señal: {}", e.getMessage(), e);
        }
    }

    /** Calcula la señal, la persiste y procesa la lógica de operaciones. */
    public synchronized SignalResponseDTO refresh() {
        List<OhlcvCandleDTO> candles = marketDataService.getCandlesMxn(CANDLES);
        SignalResponseDTO signal = strategyService.calculateSignal(candles);

        Long historyId = signalHistoryService.record (signal);
        portfolioService.ensureInitialized(Money.mxn(signal.price()));

        // La señal ya es válida: se publica antes de operar para que un fallo al procesar
        // la operación no deje el endpoint sirviendo una señal vieja.
        latest.set(signal);
        lastSuccessAt.set(Instant.now());

        try {
            tradeService.processSignal(signal, historyId);
        } catch (Exception e) {
            // El error de la operación no invalida la señal ya publicada; se registra aparte.
            log.error(
                    "La señal se calculó pero falló el procesamiento de la operación: {}",
                    e.getMessage(),
                    e);
        }
        return signal;
    }

    /**
     * Última señal calculada. Antes de la primera corrida responde SIGNAL_NOT_READY; si el cálculo
     * lleva más de 3 ciclos sin actualizarse, responde SIGNAL_STALE en vez de servir datos viejos.
     */
    public SignalResponseDTO getLatest() {
        SignalResponseDTO signal = latest.get();
        Instant success = lastSuccessAt.get();
        if (signal == null || success == null) {
            throw new SignalNotReadyException();
        }
        long ageSeconds = Duration.between(success, Instant.now()).getSeconds();
        if (ageSeconds > (intervalMs * 3) / 1000) {
            throw new SignalStaleException(ageSeconds);
        }
        return signal;
    }
}
