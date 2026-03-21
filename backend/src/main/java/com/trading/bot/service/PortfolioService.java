package com.trading.bot.service;

import com.trading.bot.dto.PortfolioDTO;
import com.trading.bot.model.PortfolioSnapshot;
import com.trading.bot.repository.PortfolioSnapshotRepository;
import com.trading.bot.repository.TradeRepository;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private static final double INITIAL_MXN = 50_000.0;

    private final PortfolioSnapshotRepository snapshotRepository;
    private final TradeRepository tradeRepository;

    public PortfolioService(PortfolioSnapshotRepository snapshotRepository,
                            TradeRepository tradeRepository) {
        this.snapshotRepository = snapshotRepository;
        this.tradeRepository    = tradeRepository;
    }

    /** Creates the initial portfolio if none exists yet. */
    public void ensureInitialized(double btcPrice) {
        if (snapshotRepository.findTopByOrderBySnapshotAtDesc().isEmpty()) {
            createSnapshot(INITIAL_MXN, 0.0, btcPrice, 0.0, snapshotRepository);
        }
    }

    /** Returns current portfolio state including unrealised P&L and stats. */
    public PortfolioDTO getPortfolio(double currentBtcPrice) {
        ensureInitialized(currentBtcPrice);

        var snap = snapshotRepository.findTopByOrderBySnapshotAtDesc().orElseThrow();

        // Unrealised P&L from any open trade
        double unrealisedPnl = tradeRepository
                .findTopByStatusOrderByOpenedAtDesc("OPEN")
                .map(t -> (t.getQuantity() * currentBtcPrice) - t.getInvestedMxn())
                .orElse(0.0);

        double totalValue   = snap.getMxnBalance()
                              + (snap.getBtcBalance() * currentBtcPrice)
                              + Math.max(0, unrealisedPnl);
        double totalReturn  = totalValue - INITIAL_MXN;
        double totalRetPct  = (totalReturn / INITIAL_MXN) * 100;

        long closedCount  = tradeRepository.countByStatus("CLOSED");
        long winningCount = tradeRepository.countByStatusAndProfitLossGreaterThan("CLOSED", 0.0);
        double winRate    = closedCount > 0 ? (double) winningCount / closedCount * 100 : 0;

        return new PortfolioDTO(
                INITIAL_MXN,
                snap.getMxnBalance(),
                snap.getBtcBalance(),
                currentBtcPrice,
                totalValue,
                unrealisedPnl,
                totalReturn,
                totalRetPct,
                (int) closedCount,
                (int) winningCount,
                winRate);
    }

    /** Package-level helper used by TradeService to persist snapshots. */
    static void createSnapshot(double mxn, double btc, double btcPrice,
                                double unrealisedPnl,
                                PortfolioSnapshotRepository repo) {
        repo.save(PortfolioSnapshot.builder()
                .mxnBalance(mxn)
                .btcBalance(btc)
                .btcPrice(btcPrice)
                .totalValueMxn(mxn + btc * btcPrice)
                .unrealizedPnl(unrealisedPnl)
                .build());
    }
}
