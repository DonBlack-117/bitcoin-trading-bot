package com.trading.bot.controller;

import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.model.SignalHistory;
import com.trading.bot.service.BitsoService;
import com.trading.bot.service.PortfolioService;
import com.trading.bot.service.SignalHistoryService;
import com.trading.bot.service.StrategyService;
import com.trading.bot.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SignalController {

    private final BitsoService bitsoService;
    private final StrategyService strategyService;
    private final SignalHistoryService signalHistoryService;
    private final TradeService tradeService;
    private final PortfolioService portfolioService;

    public SignalController(BitsoService bitsoService,
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

    @GetMapping("/signal")
    public ResponseEntity<SignalResponseDTO> getSignal() {
        try {
            List<OhlcvCandleDTO> candles = bitsoService.getOhlcv(200);
            SignalResponseDTO signal = strategyService.calculateSignal(candles);

            // Persist signal
            signalHistoryService.saveSignal(
                    signal.signal(), signal.price(), signal.confidence(),
                    signal.scoreBuy(), signal.scoreSell(), signal.votes());

            // Initialise portfolio on first run, then process trade logic
            portfolioService.ensureInitialized(signal.price());
            tradeService.processSignal(signal);

            return ResponseEntity.ok(signal);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<SignalHistory>> getHistory() {
        try {
            return ResponseEntity.ok(signalHistoryService.getRecentSignals());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
