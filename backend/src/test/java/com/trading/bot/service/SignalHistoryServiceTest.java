package com.trading.bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.bot.domain.SignalType;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.model.SignalHistory;
import com.trading.bot.repository.SignalHistoryRepository;
import com.trading.bot.support.TestData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignalHistoryServiceTest {

    private final SignalHistoryRepository repository = mock(SignalHistoryRepository.class);
    private final SignalHistoryService service = new SignalHistoryService(repository, new ObjectMapper(), TestData.CLOCK);

    private static SignalResponseDTO signal(SignalType type, Map<String, Integer> votes) {
        return new SignalResponseDTO(type, 62, true, "", votes, Map.of(), 50, 1_480_000, 1, 7, TestData.CLOCK.instant());
    }

    @Test
    void skipsConsecutiveDuplicateSignalsAndReturnsTheCurrentRow() {
        when(repository.findTopByOrderByTimestampDesc())
                .thenReturn(Optional.of(SignalHistory.builder().id(4L).senal(SignalType.COMPRAR).build()));

        Long id = service.record(signal(SignalType.COMPRAR, Map.of("macd", 2)));

        assertThat(id).isEqualTo(4L);
        verify(repository, never()).save(any());
    }

    @Test
    void savesWhenTheSignalChanges() {
        when(repository.findTopByOrderByTimestampDesc())
                .thenReturn(Optional.of(SignalHistory.builder().id(4L).senal(SignalType.COMPRAR).build()));
        when(repository.save(any())).thenAnswer(inv -> {
            SignalHistory s = inv.getArgument(0);
            s.setId(5L);
            return s;
        });
        Map<String, Integer> votes = new LinkedHashMap<>();
        votes.put("macd", -3);

        Long id = service.record(signal(SignalType.VENDER, votes));

        ArgumentCaptor<SignalHistory> saved = ArgumentCaptor.forClass(SignalHistory.class);
        verify(repository).save(saved.capture());
        assertThat(id).isEqualTo(5L);
        assertThat(saved.getValue().getSenal()).isEqualTo(SignalType.VENDER);
        assertThat(saved.getValue().getConfianza()).isEqualTo(62);
        assertThat(saved.getValue().getPrecio()).isEqualByComparingTo("1480000.00");
        assertThat(saved.getValue().getStrategyBreakdown()).isEqualTo("{\"macd\":-3}");
    }

    @Test
    void savesTheFirstSignal() {
        when(repository.findTopByOrderByTimestampDesc()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(signal(SignalType.MANTENER, Map.of()));

        verify(repository).save(any(SignalHistory.class));
    }
}
