package com.trading.bot.controller;

import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.model.SignalHistory;
import com.trading.bot.service.SignalHistoryService;
import com.trading.bot.service.SignalScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SignalController {

    private static final Logger log = LoggerFactory.getLogger(SignalController.class);

    private final SignalScheduler signalScheduler;
    private final SignalHistoryService signalHistoryService;

    public SignalController(SignalScheduler signalScheduler,
                            SignalHistoryService signalHistoryService) {
        this.signalScheduler = signalScheduler;
        this.signalHistoryService = signalHistoryService;
    }

    @GetMapping("/signal")
    public ResponseEntity<SignalResponseDTO> getSignal() {
        try {
            return ResponseEntity.ok(signalScheduler.getLatest());
        } catch (Exception e) {
            log.error("GET /api/signal failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<SignalHistory>> getHistory() {
        try {
            return ResponseEntity.ok(signalHistoryService.getRecentSignals());
        } catch (Exception e) {
            log.error("GET /api/history failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
