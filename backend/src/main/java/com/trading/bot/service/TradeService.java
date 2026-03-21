package com.trading.bot.service;

import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.dto.TradeDTO;
import com.trading.bot.model.Trade;
import com.trading.bot.repository.PortfolioSnapshotRepository;
import com.trading.bot.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    public TradeService(TradeRepository tradeRepository, PortfolioSnapshotRepository snapshotRepository) {
        this.tradeRepository = tradeRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Processes the current signal: checks open trades for SL/TP hits or exit signals,
     * and opens a new trade when a high-confidence BUY signal is generated.
     */
    public void processSignal(SignalResponseDTO signal) {
        double currentPrice = signal.price();
        double atr = signal.indicators().getOrDefault("atr", currentPrice * 0.01);

        Optional<Trade> openOpt = tradeRepository.findTopByStatusOrderByOpenedAtDesc("OPEN");

        if (openOpt.isPresent()) {
            Trade trade = openOpt.get();
            String closeReason = null;

            if (trade.getStopLoss() != null && currentPrice <= trade.getStopLoss()) {
                closeReason = "STOP_LOSS";
            } else if (trade.getTakeProfit() != null && currentPrice >= trade.getTakeProfit()) {
                closeReason = "TAKE_PROFIT";
            } else if ("VENDER".equals(signal.signal()) && signal.confidence() >= 60) {
                closeReason = "SIGNAL";
            }

            if (closeReason != null) {
                closeTrade(trade, currentPrice, closeReason);
            }
        } else {
            if ("COMPRAR".equals(signal.signal()) && signal.confidence() >= 60) {
                openTrade(currentPrice, atr);
            }
        }
    }

    private void openTrade(double price, double atr) {
        var portfolioOpt = snapshotRepository.findTopByOrderBySnapshotAtDesc();
        if (portfolioOpt.isEmpty()) return;

        double availableMxn = portfolioOpt.get().getMxnBalance();
        if (availableMxn < 100) return;

        double investMxn = Math.min(availableMxn * 0.10, availableMxn);
        double quantity   = investMxn / price;
        double stopLoss   = price - (atr * 2.0);
        double takeProfit = price + (atr * 3.0);

        Trade trade = Trade.builder()
                .symbol("BTC_MXN")
                .tradeType("BUY")
                .entryPrice(price)
                .quantity(quantity)
                .investedMxn(investMxn)
                .stopLoss(stopLoss)
                .takeProfit(takeProfit)
                .status("OPEN")
                .openedAt(LocalDateTime.now())
                .build();
        tradeRepository.save(trade);

        // Deduct from portfolio
        var snap = portfolioOpt.get();
        PortfolioService.createSnapshot(
                snap.getMxnBalance() - investMxn,
                snap.getBtcBalance() + quantity,
                price,
                0.0,
                snapshotRepository);
    }

    private void closeTrade(Trade trade, double currentPrice, String reason) {
        double received    = trade.getQuantity() * currentPrice;
        double profitLoss  = received - trade.getInvestedMxn();
        double plPct       = (profitLoss / trade.getInvestedMxn()) * 100;

        trade.setExitPrice(currentPrice);
        trade.setStatus("CLOSED");
        trade.setCloseReason(reason);
        trade.setProfitLoss(profitLoss);
        trade.setProfitLossPct(plPct);
        trade.setClosedAt(LocalDateTime.now());
        tradeRepository.save(trade);

        // Return proceeds to portfolio
        var snapOpt = snapshotRepository.findTopByOrderBySnapshotAtDesc();
        if (snapOpt.isEmpty()) return;
        var snap = snapOpt.get();
        double newBtc = Math.max(0, snap.getBtcBalance() - trade.getQuantity());
        PortfolioService.createSnapshot(
                snap.getMxnBalance() + received,
                newBtc,
                currentPrice,
                0.0,
                snapshotRepository);
    }

    public List<TradeDTO> getRecentTrades(double currentPrice) {
        return tradeRepository.findTop20ByOrderByOpenedAtDesc().stream()
                .map(t -> toDTO(t, currentPrice))
                .collect(Collectors.toList());
    }

    public Optional<TradeDTO> getOpenTrade(double currentPrice) {
        return tradeRepository.findTopByStatusOrderByOpenedAtDesc("OPEN")
                .map(t -> toDTO(t, currentPrice));
    }

    private TradeDTO toDTO(Trade t, double currentPrice) {
        double unrealizedPnl    = 0;
        double unrealizedPnlPct = 0;
        if ("OPEN".equals(t.getStatus())) {
            unrealizedPnl    = (t.getQuantity() * currentPrice) - t.getInvestedMxn();
            unrealizedPnlPct = (unrealizedPnl / t.getInvestedMxn()) * 100;
        }
        return new TradeDTO(
                t.getId(), t.getSymbol(), t.getTradeType(),
                t.getEntryPrice(), t.getExitPrice(),
                t.getQuantity(), t.getInvestedMxn(),
                t.getStopLoss(), t.getTakeProfit(),
                t.getStatus(), t.getCloseReason(),
                t.getProfitLoss(), t.getProfitLossPct(),
                currentPrice, unrealizedPnl, unrealizedPnlPct,
                t.getOpenedAt(), t.getClosedAt());
    }
}
