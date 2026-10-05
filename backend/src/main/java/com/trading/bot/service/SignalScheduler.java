package com.trading.bot.service;

import com.trading.bot.domain.Money;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.exception.SignalNotReadyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    public SignalScheduler(MarketDataService marketDataService,
                           StrategyService strategyService,
                           SignalHistoryService signalHistoryService,
                           TradeService tradeService,
                           PortfolioService portfolioService) {
        this.marketDataService = marketDataService;
        this.strategyService = strategyService;
        this.signalHistoryService = signalHistoryService;
        this.tradeService = tradeService;
        this.portfolioService = portfolioService;
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

        Long historyId = signalHistoryService.record(signal);
        portfolioService.ensureInitialized(Money.mxn(signal.price()));
        tradeService.processSignal(signal, historyId);

        latest.set(signal);
        return signal;
    }

    /** Última señal calculada. Antes de la primera corrida responde SIGNAL_NOT_READY. */
    public SignalResponseDTO getLatest() {
        SignalResponseDTO signal = latest.get();
        if (signal == null) {
            throw new SignalNotReadyException();
        }
        return signal;
    }
}
