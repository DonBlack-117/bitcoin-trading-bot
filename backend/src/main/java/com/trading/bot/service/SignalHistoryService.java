package com.trading.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.bot.model.SignalHistory;
import com.trading.bot.repository.SignalHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SignalHistoryService {

    private final SignalHistoryRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SignalHistoryService(SignalHistoryRepository repository) {
        this.repository = repository;
    }

    public void saveSignal(String senal, double precio, int confianza,
                           int scoreBuy, int scoreSell,
                           Map<String, Integer> votes) {
        // Avoid consecutive duplicates
        SignalHistory last = repository.findTopByOrderByTimestampDesc();
        if (last != null && last.getSenal().equals(senal)) return;

        String breakdown = null;
        try {
            breakdown = objectMapper.writeValueAsString(votes);
        } catch (JsonProcessingException ignored) {}

        SignalHistory signal = SignalHistory.builder()
                .timestamp(LocalDateTime.now())
                .senal(senal)
                .precio(precio)
                .confianza(confianza)
                .scoreBuy(scoreBuy)
                .scoreSell(scoreSell)
                .strategyBreakdown(breakdown)
                .build();

        repository.save(signal);
    }

    public List<SignalHistory> getRecentSignals() {
        return repository.findTop8ByOrderByTimestampDesc();
    }
}
