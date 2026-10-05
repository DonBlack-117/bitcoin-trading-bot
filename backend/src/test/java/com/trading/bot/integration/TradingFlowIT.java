package com.trading.bot.integration;

import com.trading.bot.domain.CloseReason;
import com.trading.bot.domain.SignalType;
import com.trading.bot.domain.TradeStatus;
import com.trading.bot.model.Trade;
import com.trading.bot.repository.PortfolioSnapshotRepository;
import com.trading.bot.repository.SignalHistoryRepository;
import com.trading.bot.repository.TradeRepository;
import com.trading.bot.service.PortfolioService;
import com.trading.bot.service.SignalHistoryService;
import com.trading.bot.service.TradeService;
import com.trading.bot.support.TestData;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

/** Arranca la app completa contra MariaDB: Flyway crea las tablas y Hibernate las valida. */
@SpringBootTest
class TradingFlowIT extends MariaDbContainerSupport {

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired TradeService tradeService;
    @Autowired SignalHistoryService signalHistoryService;
    @Autowired TradeRepository trades;
    @Autowired PortfolioSnapshotRepository snapshots;
    @Autowired SignalHistoryRepository signals;
    @MockitoSpyBean PortfolioService portfolioService;

    @BeforeEach
    void cleanTables() {
        trades.deleteAll();
        snapshots.deleteAll();
        signals.deleteAll();
    }

    @Test
    void migrationsCreateDecimalColumns() {
        assertThat(flyway.info().applied()).extracting(m -> m.getVersion().getVersion()).containsExactly("1", "2");
        String type = jdbc.queryForObject("""
                SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'tradingbot' AND TABLE_NAME = 'trades' AND COLUMN_NAME = 'quantity'""", String.class);
        assertThat(type).isEqualTo("decimal(19,8)");
    }

    @Test
    void buyThenTakeProfitLeavesTheBalanceConsistent() {
        Long buyId = signalHistoryService.record(TestData.signal(SignalType.COMPRAR, 70, true, 1_500_000, 20_000));
        portfolioService.ensureInitialized(new BigDecimal("1500000"));
        tradeService.processSignal(TestData.signal(SignalType.COMPRAR, 70, true, 1_500_000, 20_000), buyId);

        Trade open = trades.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN).orElseThrow();
        assertThat(open.getQuantity()).isEqualByComparingTo("0.00333333");
        assertThat(open.getEntrySignalId()).isEqualTo(buyId);

        var afterBuy = portfolioService.getPortfolio(1_500_000);
        assertThat(afterBuy.mxnBalance()).isEqualByComparingTo("45000.00");
        // 45 000 + 0.00333333 × 1 500 000 = 49 999.995, que redondea a 50 000.00
        assertThat(afterBuy.totalValueMxn()).isEqualByComparingTo("50000.00");

        tradeService.processSignal(TestData.signal(SignalType.MANTENER, 50, false, 1_560_000, 20_000), buyId);

        Trade closed = trades.findById(open.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(TradeStatus.CLOSED);
        assertThat(closed.getCloseReason()).isEqualTo(CloseReason.TAKE_PROFIT);
        assertThat(closed.getProfitLoss()).isEqualByComparingTo("199.99");

        var afterSell = portfolioService.getPortfolio(1_560_000);
        assertThat(afterSell.mxnBalance()).isEqualByComparingTo("50199.99");
        assertThat(afterSell.btcBalance()).isEqualByComparingTo("0");
        assertThat(afterSell.totalTrades()).isEqualTo(1);
        assertThat(afterSell.winningTrades()).isEqualTo(1);
    }

    @Test
    void tradeIsNotSavedWhenTheBalanceUpdateFails() {
        portfolioService.ensureInitialized(new BigDecimal("1500000"));
        doThrow(new IllegalStateException("falla simulada")).when(portfolioService).record(any(), any(), any());
        try {
            assertThatThrownBy(() -> tradeService.processSignal(
                    TestData.signal(SignalType.COMPRAR, 70, true, 1_500_000, 20_000), null))
                    .hasMessage("falla simulada");

            assertThat(trades.count()).isZero();
            assertThat(snapshots.count()).isEqualTo(1);
        } finally {
            doCallRealMethod().when(portfolioService).record(any(), any(), any());
        }
    }

    @Test
    void historyStoresOnlySignalChanges() {
        signalHistoryService.record(TestData.signal(SignalType.COMPRAR, 70, true, 1_500_000, 1));
        signalHistoryService.record(TestData.signal(SignalType.COMPRAR, 72, true, 1_501_000, 1));
        signalHistoryService.record(TestData.signal(SignalType.VENDER, 65, true, 1_490_000, 1));

        assertThat(signalHistoryService.getRecentSignals())
                .extracting(s -> s.signal())
                .containsExactlyInAnyOrder(SignalType.COMPRAR, SignalType.VENDER);
    }
}
