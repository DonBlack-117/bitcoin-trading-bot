package com.trading.bot.service;

import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.SignalResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recalcula la señal en segundo plano, la guarda y procesa las operaciones simuladas.
 * GET /api/signal solo lee la última señal, así el trabajo no depende de cuántas pestañas estén abiertas.
 */
@Component
public class SignalScheduler {

    private static final Logger log = LoggerFactory.getLogger(SignalScheduler.class);

    private final BitsoService bitsoService;
    private final StrategyService strategyService;
    private final SignalHistoryService signalHistoryService;
    private final TradeService tradeService;
    private final PortfolioService portfolioService;

    private final AtomicReference<SignalResponseDTO> latest = new AtomicReference<>();

    public SignalScheduler(BitsoService bitsoService,
                           StrategyService strategyService,
                           SignalHistoryService signalHistoryService,
                           TradeService tradeService,
                           PortfolioService portfolioService) {
        this.bitsoService = bitsoService;
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
            log.error("Scheduled signal update failed: {}", e.getMessage());
        }
    }

    /** Calcula la señal, la persiste y procesa la lógica de operaciones. */
    public synchronized SignalResponseDTO refresh() {
        List<OhlcvCandleDTO> candles = bitsoService.getOhlcv(200);
        SignalResponseDTO signal = strategyService.calculateSignal(candles);

        signalHistoryService.saveSignal(
                signal.signal(), signal.price(), signal.confidence(),
                signal.scoreBuy(), signal.scoreSell(), signal.votes());

        // Inicializa el portafolio la primera vez y luego procesa las operaciones
        portfolioService.ensureInitialized(signal.price());
        tradeService.processSignal(signal);

        latest.set(signal);
        return signal;
    }

    /** Última señal calculada; si todavía no hay ninguna, la calcula en ese momento. */
    public SignalResponseDTO getLatest() {
        SignalResponseDTO signal = latest.get();
        return signal != null ? signal : refresh();
    }
}
