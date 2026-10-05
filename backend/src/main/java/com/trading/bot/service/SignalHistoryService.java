package com.trading.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.bot.domain.Money;
import com.trading.bot.dto.SignalHistoryDTO;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.model.SignalHistory;
import com.trading.bot.repository.SignalHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SignalHistoryService {

    private final SignalHistoryRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SignalHistoryService(SignalHistoryRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Guarda la señal solo si cambió respecto a la anterior.
     *
     * @return id de la fila vigente: la nueva, o la anterior si la señal no cambió
     */
    @Transactional
    public Long record(SignalResponseDTO signal) {
        Optional<SignalHistory> last = repository.findTopByOrderByTimestampDesc();
        if (last.isPresent() && last.get().getSenal() == signal.signal()) {
            return last.get().getId();
        }

        SignalHistory saved = repository.save(SignalHistory.builder()
                .timestamp(LocalDateTime.now(clock))
                .senal(signal.signal())
                .precio(Money.mxn(signal.price()))
                .confianza(signal.confidence())
                .scoreBuy(signal.scoreBuy())
                .scoreSell(signal.scoreSell())
                .strategyBreakdown(toJson(signal))
                .build());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<SignalHistoryDTO> getRecentSignals() {
        return repository.findTop8ByOrderByTimestampDesc().stream().map(SignalHistoryDTO::from).toList();
    }

    private String toJson(SignalResponseDTO signal) {
        try {
            return objectMapper.writeValueAsString(signal.votes());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los votos", e);
        }
    }
}
