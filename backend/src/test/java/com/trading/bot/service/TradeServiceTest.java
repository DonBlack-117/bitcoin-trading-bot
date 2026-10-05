package com.trading.bot.service;

import com.trading.bot.domain.CloseReason;
import com.trading.bot.domain.SignalType;
import com.trading.bot.domain.TradeStatus;
import com.trading.bot.model.PortfolioSnapshot;
import com.trading.bot.model.Trade;
import com.trading.bot.repository.TradeRepository;
import com.trading.bot.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TradeServiceTest {

    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final PortfolioService portfolioService = mock(PortfolioService.class);
    private final TradeService service =
            new TradeService(tradeRepository, portfolioService, TestData.properties(), TestData.CLOCK);

    @BeforeEach
    void noOpenTradeAndFullBalance() {
        when(tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.empty());
        when(portfolioService.ensureInitialized(any())).thenReturn(snapshot("50000.00", "0"));
    }

    private static PortfolioSnapshot snapshot(String mxn, String btc) {
        return PortfolioSnapshot.builder().mxnBalance(new BigDecimal(mxn)).btcBalance(new BigDecimal(btc)).build();
    }

    private static Trade openTrade() {
        return Trade.builder()
                .id(7L)
                .status(TradeStatus.OPEN)
                .entryPrice(new BigDecimal("1500000.00"))
                .quantity(new BigDecimal("0.00333333"))
                .investedMxn(new BigDecimal("5000.00"))
                .stopLoss(new BigDecimal("1460000.00"))
                .takeProfit(new BigDecimal("1560000.00"))
                .build();
    }

    @Test
    void opensTenPercentOfTheBalanceOnAnActionableBuy() {
        service.processSignal(TestData.signal(SignalType.COMPRAR, 70, true, 1_500_000, 20_000), 3L);

        ArgumentCaptor<Trade> trade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(trade.capture());
        Trade t = trade.getValue();
        assertThat(t.getInvestedMxn()).isEqualByComparingTo("5000.00");
        assertThat(t.getQuantity()).isEqualByComparingTo("0.00333333");   // redondeo hacia abajo
        assertThat(t.getStopLoss()).isEqualByComparingTo("1460000.00");  // precio − 2 ATR
        assertThat(t.getTakeProfit()).isEqualByComparingTo("1560000.00"); // precio + 3 ATR
        assertThat(t.getEntrySignalId()).isEqualTo(3L);
        assertThat(t.getOpenedAt()).isNotNull();

        verify(portfolioService).record(
                argThat(mxn -> mxn.compareTo(new BigDecimal("45000.00")) == 0),
                argThat(btc -> btc.compareTo(new BigDecimal("0.00333333")) == 0),
                any());
    }

    @Test
    void doesNotOpenOnAWeakBuy() {
        service.processSignal(TestData.signal(SignalType.COMPRAR, 55, false, 1_500_000, 20_000), 3L);

        verify(tradeRepository, never()).save(any());
        verify(portfolioService, never()).record(any(), any(), any());
    }

    @Test
    void doesNotOpenWhenTheBalanceIsBelowTheMinimum() {
        when(portfolioService.ensureInitialized(any())).thenReturn(snapshot("99.99", "0"));

        service.processSignal(TestData.signal(SignalType.COMPRAR, 80, true, 1_500_000, 20_000), 3L);

        verify(tradeRepository, never()).save(any());
    }

    @Test
    void closesOnStopLossAndReturnsTheProceeds() {
        when(tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.of(openTrade()));
        when(portfolioService.ensureInitialized(any())).thenReturn(snapshot("45000.00", "0.00333333"));

        service.processSignal(TestData.signal(SignalType.MANTENER, 50, false, 1_450_000, 20_000), 9L);

        ArgumentCaptor<Trade> trade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(trade.capture());
        Trade t = trade.getValue();
        assertThat(t.getStatus()).isEqualTo(TradeStatus.CLOSED);
        assertThat(t.getCloseReason()).isEqualTo(CloseReason.STOP_LOSS);
        // 0.00333333 × 1 450 000 = 4833.33
        assertThat(t.getProfitLoss()).isEqualByComparingTo("-166.67");
        assertThat(t.getProfitLossPct()).isEqualByComparingTo("-3.3334");
        assertThat(t.getExitSignalId()).isEqualTo(9L);

        verify(portfolioService).record(
                argThat(mxn -> mxn.compareTo(new BigDecimal("49833.33")) == 0),
                argThat(btc -> btc.signum() == 0),
                any());
    }

    @Test
    void closesOnTakeProfit() {
        when(tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.of(openTrade()));
        when(portfolioService.ensureInitialized(any())).thenReturn(snapshot("45000.00", "0.00333333"));

        service.processSignal(TestData.signal(SignalType.COMPRAR, 90, true, 1_560_000, 20_000), 9L);

        ArgumentCaptor<Trade> trade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(trade.capture());
        assertThat(trade.getValue().getCloseReason()).isEqualTo(CloseReason.TAKE_PROFIT);
        assertThat(trade.getValue().getProfitLoss()).isEqualByComparingTo("199.99");
    }

    @Test
    void closesOnAnActionableSellButNotOnAWeakOne() {
        when(tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.of(openTrade()));
        when(portfolioService.ensureInitialized(any())).thenReturn(snapshot("45000.00", "0.00333333"));

        service.processSignal(TestData.signal(SignalType.VENDER, 55, false, 1_500_000, 20_000), 9L);
        verify(tradeRepository, never()).save(any());

        service.processSignal(TestData.signal(SignalType.VENDER, 65, true, 1_500_000, 20_000), 9L);
        ArgumentCaptor<Trade> trade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(trade.capture());
        assertThat(trade.getValue().getCloseReason()).isEqualTo(CloseReason.SIGNAL);
    }

    @Test
    void openTradeShowsUnrealisedProfit() {
        when(tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.of(openTrade()));

        var dto = service.getOpenTrade(1_530_000).orElseThrow();

        // 0.00333333 × 1 530 000 = 5099.99
        assertThat(dto.unrealizedPnl()).isEqualByComparingTo("99.99");
        assertThat(dto.currentPrice()).isEqualByComparingTo("1530000.00");
    }

    @Test
    void invalidAtrFallsBackToOnePercentOfThePrice() {
        var signal = new com.trading.bot.dto.SignalResponseDTO(SignalType.COMPRAR, 70, true, "",
                java.util.Map.of(), java.util.Map.of("atr", Double.NaN), 50, 1_500_000, 8, 0, TestData.CLOCK.instant());

        service.processSignal(signal, null);

        ArgumentCaptor<Trade> trade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(trade.capture());
        // ATR = 15 000: stop loss a 2 ATR y take profit a 3 ATR
        assertThat(trade.getValue().getStopLoss()).isEqualByComparingTo("1470000.00");
        assertThat(trade.getValue().getTakeProfit()).isEqualByComparingTo("1545000.00");
    }
}
