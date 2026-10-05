package com.trading.bot.service;

import com.trading.bot.domain.SignalType;
import com.trading.bot.dto.AnalysisRequestDTO;
import com.trading.bot.dto.CryptoDTO;
import com.trading.bot.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private final CoinMarketCapService cmc = mock(CoinMarketCapService.class);
    private final AnalysisService service = new AnalysisService(cmc);

    private void top(CryptoDTO... cryptos) {
        when(cmc.getCryptoList(100, "MXN")).thenReturn(List.of(cryptos));
    }

    @Test
    void strongRallyGivesAStrongBuy() {
        top(new CryptoDTO(2, "ETH", "Ethereum", 50_000, 1e12, 2e11, 2.0, 7.0, 15.0));

        var result = service.analyze(new AnalysisRequestDTO("eth", 1000.0));

        assertThat(result.symbol()).isEqualTo("ETH");
        assertThat(result.signal()).isEqualTo(SignalType.COMPRAR);
        assertThat(result.strong()).isTrue();
        assertThat(result.unitsToBuy()).isEqualTo(0.02);
        assertThat(result.optimisticMxn()).isGreaterThan(result.expectedMxn());
        assertThat(result.riskMxn()).isLessThan(1000);
    }

    @Test
    void flatMarketMeansHold() {
        top(new CryptoDTO(5, "SOL", "Solana", 3_000, 1e11, 1e9, 0.1, 0.2, 0.5));

        var result = service.analyze(new AnalysisRequestDTO("SOL", 500.0));

        assertThat(result.signal()).isEqualTo(SignalType.MANTENER);
        assertThat(result.strong()).isFalse();
        assertThat(result.confidence()).isEqualTo(50);
        assertThat(result.reasons()).anyMatch(r -> r.contains("esperar"));
    }

    @Test
    void missingPercentagesCountAsZero() {
        top(new CryptoDTO(9, "XYZ", "Ejemplo", 10, 1e9, 1e7, null, null, null));

        var result = service.analyze(new AnalysisRequestDTO("XYZ", 100.0));

        assertThat(result.pct24h()).isZero();
        assertThat(result.signal()).isEqualTo(SignalType.MANTENER);
    }

    @Test
    void unknownSymbolIsNotFound() {
        top(new CryptoDTO(1, "BTC", "Bitcoin", 1_500_000, 3e13, 1e12, 0.0, 0.0, 0.0));

        assertThatThrownBy(() -> service.analyze(new AnalysisRequestDTO("DOGE", 100.0)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("DOGE");
    }
}
