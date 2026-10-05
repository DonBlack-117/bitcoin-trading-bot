package com.trading.bot.controller;

import com.trading.bot.dto.SignalHistoryDTO;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.service.SignalHistoryService;
import com.trading.bot.service.SignalScheduler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SignalController {

    private final SignalScheduler signalScheduler;
    private final SignalHistoryService signalHistoryService;

    public SignalController(SignalScheduler signalScheduler,
                            SignalHistoryService signalHistoryService) {
        this.signalScheduler = signalScheduler;
        this.signalHistoryService = signalHistoryService;
    }

    @GetMapping("/signal")
    public SignalResponseDTO getSignal() {
        return signalScheduler.getLatest();
    }

    @GetMapping("/history")
    public List<SignalHistoryDTO> getHistory() {
        return signalHistoryService.getRecentSignals();
    }
}
