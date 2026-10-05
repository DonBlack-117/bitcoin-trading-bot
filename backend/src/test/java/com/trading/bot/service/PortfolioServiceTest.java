package com.trading.bot.service;

import com.trading.bot.domain.TradeStatus;
import com.trading.bot.model.PortfolioSnapshot;
import com.trading.bot.model.Trade;
import com.trading.bot.repository.PortfolioSnapshotRepository;
import com.trading.bot.repository.TradeRepository;
import com.trading.bot.support.TestData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PortfolioServiceTest {

    private final PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
    private final TradeRepository trades = mock(TradeRepository.class);
    private final PortfolioService service = new PortfolioService(snapshots, trades, TestData.properties(), TestData.CLOCK);

    @Test
    void openTradeProfitIsNotCountedTwice() {
        // Después de comprar 0.00333333 BTC con 5000 MXN
        when(snapshots.findTopByOrderBySnapshotAtDescIdDesc()).thenReturn(Optional.of(PortfolioSnapshot.builder()
                .mxnBalance(new BigDecimal("45000.00"))
                .btcBalance(new BigDecimal("0.00333333"))
                .build()));
        when(trades.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.of(Trade.builder()
                .quantity(new BigDecimal("0.00333333"))
                .investedMxn(new BigDecimal("5000.00"))
                .build()));

        var portfolio = service.getPortfolio(1_800_000);

        // 45 000 + 0.00333333 × 1 800 000 = 50 999.99; antes sumaba otra vez los 999.99 de ganancia
        assertThat(portfolio.totalValueMxn()).isEqualByComparingTo("50999.99");
        assertThat(portfolio.unrealizedPnl()).isEqualByComparingTo("999.99");
        assertThat(portfolio.totalReturn()).isEqualByComparingTo("999.99");
        assertThat(portfolio.totalReturnPct()).isEqualByComparingTo("2.00");
    }

    @Test
    void openTradeLossLowersTheTotal() {
        when(snapshots.findTopByOrderBySnapshotAtDescIdDesc()).thenReturn(Optional.of(PortfolioSnapshot.builder()
                .mxnBalance(new BigDecimal("45000.00"))
                .btcBalance(new BigDecimal("0.00333333"))
                .build()));
        when(trades.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.empty());

        var portfolio = service.getPortfolio(1_100_000);

        // 45 000 + 0.00333333 × 1 100 000 = 48 666.66
        assertThat(portfolio.totalValueMxn()).isEqualByComparingTo("48666.66");
        assertThat(portfolio.totalReturn()).isEqualByComparingTo("-1333.34");
    }

    @Test
    void createsTheInitialPortfolioOnce() {
        when(snapshots.findTopByOrderBySnapshotAtDescIdDesc()).thenReturn(Optional.empty());
        when(snapshots.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PortfolioSnapshot created = service.ensureInitialized(new BigDecimal("1500000"));

        assertThat(created.getMxnBalance()).isEqualByComparingTo("50000.00");
        assertThat(created.getBtcBalance()).isEqualByComparingTo("0");
        assertThat(created.getSnapshotAt()).isNotNull();
        verify(snapshots).save(any());
    }

    @Test
    void winRateCountsOnlyClosedTradesWithProfit() {
        when(snapshots.findTopByOrderBySnapshotAtDescIdDesc()).thenReturn(Optional.of(PortfolioSnapshot.builder()
                .mxnBalance(new BigDecimal("50000.00")).btcBalance(BigDecimal.ZERO).build()));
        when(trades.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.empty());
        when(trades.countByStatus(TradeStatus.CLOSED)).thenReturn(3L);
        when(trades.countByStatusAndProfitLossGreaterThan(eq(TradeStatus.CLOSED), any())).thenReturn(2L);

        var portfolio = service.getPortfolio(1_500_000);

        assertThat(portfolio.totalTrades()).isEqualTo(3);
        assertThat(portfolio.winRate()).isEqualByComparingTo("66.67");
    }

    @Test
    void recordComputesTheTotalValue() {
        when(snapshots.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(new BigDecimal("45000"), new BigDecimal("0.00333333"), new BigDecimal("1600000"));

        ArgumentCaptor<PortfolioSnapshot> saved = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshots).save(saved.capture());
        assertThat(saved.getValue().getTotalValueMxn()).isEqualByComparingTo("50333.33");
    }

    @Test
    void readingThePortfolioNeverWrites() {
        when(snapshots.findTopByOrderBySnapshotAtDescIdDesc()).thenReturn(Optional.empty());
        when(trades.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)).thenReturn(Optional.empty());

        var portfolio = service.getPortfolio(1_500_000);

        assertThat(portfolio.mxnBalance()).isEqualByComparingTo("50000.00");
        assertThat(portfolio.totalValueMxn()).isEqualByComparingTo("50000.00");
        verify(snapshots, never()).save(any());
    }
}
