package com.trading.bot.service;

import com.trading.bot.config.BotProperties;
import com.trading.bot.domain.CloseReason;
import com.trading.bot.domain.Money;
import com.trading.bot.domain.SignalType;
import com.trading.bot.domain.TradeStatus;
import com.trading.bot.domain.TradeType;
import com.trading.bot.dto.SignalResponseDTO;
import com.trading.bot.dto.TradeDTO;
import com.trading.bot.model.PortfolioSnapshot;
import com.trading.bot.model.Trade;
import com.trading.bot.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TradeRepository tradeRepository;
    private final PortfolioService portfolioService;
    private final BotProperties properties;
    private final Clock clock;

    public TradeService(TradeRepository tradeRepository, PortfolioService portfolioService,
                        BotProperties properties, Clock clock) {
        this.tradeRepository = tradeRepository;
        this.portfolioService = portfolioService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Revisa la operación abierta (stop loss, take profit o señal de venta) o abre una nueva con
     * una señal de compra operable. La operación y el saldo se guardan juntos o no se guarda nada.
     *
     * @param signalHistoryId fila de signals_history vigente, para ligarla a la operación
     */
    @Transactional
    public void processSignal(SignalResponseDTO signal, Long signalHistoryId) {
        BigDecimal price = Money.mxn(signal.price());
        Double atrValue = signal.indicators().get("atr");
        // Con velas planas o datos raros el ATR puede salir 0, NaN o infinito: se usa el 1 % del precio
        double atr = atrValue != null && Double.isFinite(atrValue) && atrValue > 0 ? atrValue : signal.price() * 0.01;

        Optional<Trade> open = tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN);
        if (open.isPresent()) {
            closeReason(open.get(), price, signal)
                    .ifPresent(reason -> closeTrade(open.get(), price, reason, signalHistoryId));
        } else if (signal.signal() == SignalType.COMPRAR && signal.actionable()) {
            openTrade(price, atr, signalHistoryId);
        }
    }

    /**
     * El bot revisa cada 30 s, así que el cierre es al precio de esa revisión y no al nivel exacto
     * del stop loss o del take profit: si el precio salta, la pérdida puede pasar de 2 ATR.
     */
    private Optional<CloseReason> closeReason(Trade trade, BigDecimal price, SignalResponseDTO signal) {
        if (trade.getStopLoss() != null && price.compareTo(trade.getStopLoss()) <= 0) {
            return Optional.of(CloseReason.STOP_LOSS);
        }
        if (trade.getTakeProfit() != null && price.compareTo(trade.getTakeProfit()) >= 0) {
            return Optional.of(CloseReason.TAKE_PROFIT);
        }
        if (signal.signal() == SignalType.VENDER && signal.actionable()) {
            return Optional.of(CloseReason.SIGNAL);
        }
        return Optional.empty();
    }

    private void openTrade(BigDecimal price, double atr, Long signalHistoryId) {
        PortfolioSnapshot snap = portfolioService.ensureInitialized(price);
        BigDecimal available = snap.getMxnBalance();
        if (available.compareTo(properties.minOrderMxn()) < 0) {
            log.info("No se abre operación: saldo de {} MXN menor al mínimo", available);
            return;
        }

        BigDecimal investMxn = Money.mxn(available.multiply(properties.positionFraction()));
        BigDecimal quantity  = investMxn.divide(price, Money.BTC_SCALE, RoundingMode.DOWN);
        BigDecimal stopLoss   = Money.mxn(price.subtract(BigDecimal.valueOf(atr * properties.stopLossAtr())));
        BigDecimal takeProfit = Money.mxn(price.add(BigDecimal.valueOf(atr * properties.takeProfitAtr())));

        tradeRepository.save(Trade.builder()
                .symbol("BTC_MXN")
                .tradeType(TradeType.BUY)
                .entryPrice(price)
                .quantity(quantity)
                .investedMxn(investMxn)
                .stopLoss(stopLoss)
                .takeProfit(takeProfit)
                .status(TradeStatus.OPEN)
                .openedAt(LocalDateTime.now(clock))
                .entrySignalId(signalHistoryId)
                .build());

        portfolioService.record(available.subtract(investMxn), snap.getBtcBalance().add(quantity), price);
        log.info("Operación abierta: {} BTC a {} MXN (SL {}, TP {})", quantity, price, stopLoss, takeProfit);
    }

    private void closeTrade(Trade trade, BigDecimal price, CloseReason reason, Long signalHistoryId) {
        BigDecimal received   = Money.mxn(trade.getQuantity().multiply(price));
        BigDecimal profitLoss = received.subtract(trade.getInvestedMxn());

        trade.setExitPrice(price);
        trade.setStatus(TradeStatus.CLOSED);
        trade.setCloseReason(reason);
        trade.setProfitLoss(profitLoss);
        trade.setProfitLossPct(profitLoss.multiply(HUNDRED).divide(trade.getInvestedMxn(), 4, RoundingMode.HALF_UP));
        trade.setClosedAt(LocalDateTime.now(clock));
        trade.setExitSignalId(signalHistoryId);
        tradeRepository.save(trade);

        PortfolioSnapshot snap = portfolioService.ensureInitialized(price);
        BigDecimal btcLeft = snap.getBtcBalance().subtract(trade.getQuantity()).max(BigDecimal.ZERO);
        portfolioService.record(snap.getMxnBalance().add(received), btcLeft, price);
        log.info("Operación {} cerrada por {}: {} MXN", trade.getId(), reason, profitLoss);
    }

    @Transactional(readOnly = true)
    public List<TradeDTO> getRecentTrades(double currentPrice) {
        BigDecimal price = Money.mxn(currentPrice);
        return tradeRepository.findTop20ByOrderByOpenedAtDesc().stream()
                .map(t -> toDTO(t, price))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TradeDTO> getOpenTrade(double currentPrice) {
        BigDecimal price = Money.mxn(currentPrice);
        return tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)
                .map(t -> toDTO(t, price));
    }

    private TradeDTO toDTO(Trade t, BigDecimal currentPrice) {
        BigDecimal unrealizedPnl    = null;
        BigDecimal unrealizedPnlPct = null;
        if (t.getStatus() == TradeStatus.OPEN) {
            unrealizedPnl    = Money.mxn(t.getQuantity().multiply(currentPrice).subtract(t.getInvestedMxn()));
            unrealizedPnlPct = unrealizedPnl.multiply(HUNDRED).divide(t.getInvestedMxn(), 4, RoundingMode.HALF_UP);
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
