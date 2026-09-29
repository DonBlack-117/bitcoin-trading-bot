package com.trading.bot.service;

import com.trading.bot.model.SignalHistory;
import com.trading.bot.repository.SignalHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignalHistoryServiceTest {

    private final SignalHistoryRepository repository = mock(SignalHistoryRepository.class);
    private final SignalHistoryService service = new SignalHistoryService(repository);

    @Test
    void skipsConsecutiveDuplicateSignals() {
        when(repository.findTopByOrderByTimestampDesc())
                .thenReturn(SignalHistory.builder().senal("COMPRAR").build());

        service.saveSignal("COMPRAR", 1_500_000, 55, 3, 1, Map.of("macd", 2));

        verify(repository, never()).save(any());
    }

    @Test
    void savesWhenTheSignalChanges() {
        when(repository.findTopByOrderByTimestampDesc())
                .thenReturn(SignalHistory.builder().senal("COMPRAR").build());

        service.saveSignal("VENDER", 1_480_000, 62, 1, 7, Map.of("macd", -3));

        ArgumentCaptor<SignalHistory> saved = ArgumentCaptor.forClass(SignalHistory.class);
        verify(repository).save(saved.capture());
        assertEquals("VENDER", saved.getValue().getSenal());
        assertEquals(62, saved.getValue().getConfianza());
        assertEquals("{\"macd\":-3}", saved.getValue().getStrategyBreakdown());
    }

    @Test
    void savesTheFirstSignal() {
        when(repository.findTopByOrderByTimestampDesc()).thenReturn(null);

        service.saveSignal("MANTENER", 1_490_000, 50, 0, 0, Map.of());

        verify(repository).save(any(SignalHistory.class));
    }
}
