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
        double atr = resolveAtr(signal);

        Optional<Trade> open = tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN);
        if (open.isPresent()) {
            closeReason(open.get(), price, signal)
                    .ifPresent(reason -> closeTrade(open.get(), price, reason, signalHistoryId));
        } else if (signal.signal() == SignalType.COMPRAR && signal.actionable() && !inCooldown()) {
            openTrade(price, atr, signalHistoryId);
        }
    }

    /**
     * ATR en pesos para fijar el stop loss y el take profit. Con velas planas o datos raros puede salir
     * 0, NaN o infinito: entonces se usa el 1 % del precio. Se acota al 10 % del precio para que una mecha
     * anómala no deje el stop loss inalcanzable.
     */
    private double resolveAtr(SignalResponseDTO signal) {
        Double atrValue = signal.indicators().get("atr");
        double atr;
        if (atrValue != null && Double.isFinite(atrValue) && atrValue > 0) {
            atr = atrValue;
        } else {
            atr = signal.price() * 0.01;
            log.warn("ATR inválido ({}); se usa el 1 % del precio ({})", atrValue, atr);
        }
        double cap = signal.price() * 0.10;
        if (atr > cap) {
            log.warn("ATR {} supera el 10 % del precio; se acota a {}", atr, cap);
            atr = cap;
        }
        return atr;
    }

    /**
     * Tras cerrar por stop loss o take profit no se reabre de inmediato: con la vela de 1 h casi igual,
     * el bot abriría otra operación al mismo precio en el siguiente tick (30 s) y repetiría la pérdida.
     */
    private boolean inCooldown() {
        if (properties.cooldownMinutes() <= 0) {
            return false;
        }
        return tradeRepository.findTopByStatusOrderByClosedAtDesc(TradeStatus.CLOSED)
                .filter(t -> t.getCloseReason() == CloseReason.STOP_LOSS
                                || t.getCloseReason() == CloseReason.TAKE_PROFIT)
                .filter(t -> t.getClosedAt() != null)
                .map(t -> {
                    LocalDateTime until = t.getClosedAt().plusMinutes(properties.cooldownMinutes());
                    boolean waiting = LocalDateTime.now(clock).isBefore(until);
                    if (waiting) {
                        log.info("En enfriamiento tras {} de la operación {}; no se reabre hasta {}",
                                t.getCloseReason(), t.getId(), until);
                    }
                    return waiting;
                })
                .orElse(false);
    }

    /**
     * El bot revisa cada 30 s, así que el cierre es al precio de esa revisión y no al nivel exacto
     * del stop loss o del take profit: si el precio salta, la pérdida puede pasar de 2 ATR.
     */
    private Optional<CloseReason> closeReason(
            Trade trade, BigDecimal price, SignalResponseDTO signal) {
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

        BigDecimal budget = Money.mxn(available.multiply(properties.positionFraction()));
        BigDecimal quantity = budget.divide(price, Money.BTC_SCALE, RoundingMode.DOWN);
        // El costo base es lo que cuestan las BTC realmente compradas, no el presupuesto: así la
        // operación no nace con una pérdida de polvo por el redondeo de la cantidad.
        BigDecimal investMxn = Money.mxn(quantity.multiply(price));
        if (quantity.signum() <= 0 || investMxn.signum() <= 0) {
            log.info(
                    "No se abre operación: el presupuesto de {} MXN no alcanza ni 1 satoshi a {} MXN",
                    budget,
                    price);
            return;
        }
        BigDecimal stopLoss = Money.mxn(
                price.subtract(BigDecimal.valueOf(atr * properties.stopLossAtr())));
        BigDecimal takeProfit = Money.mxn(
                price.add(BigDecimal.valueOf(atr * properties.takeProfitAtr())));

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

        portfolioService.record (
                available.subtract(investMxn), snap.getBtcBalance().add(quantity), price);
        log.info(
                "Operación abierta: {} BTC a {} MXN (SL {}, TP {})",
                quantity,
                price,
                stopLoss,
                takeProfit);
    }

    private void closeTrade(
            Trade trade, BigDecimal price, CloseReason reason, Long signalHistoryId) {
        BigDecimal received = Money.mxn(trade.getQuantity().multiply(price));
        BigDecimal profitLoss = received.subtract(trade.getInvestedMxn());

        trade.setExitPrice(price);
        trade.setStatus(TradeStatus.CLOSED);
        trade.setCloseReason(reason);
        trade.setProfitLoss(profitLoss);
        trade.setProfitLossPct(
                profitLoss.multiply(HUNDRED).divide(
                        trade.getInvestedMxn(), 4, RoundingMode.HALF_UP));
        trade.setClosedAt(LocalDateTime.now(clock));
        trade.setExitSignalId(signalHistoryId);
        tradeRepository.save(trade);

        PortfolioSnapshot snap = portfolioService.ensureInitialized(price);
        BigDecimal btcLeft = snap.getBtcBalance()
                .subtract(trade.getQuantity())
                .max(BigDecimal.ZERO);
        portfolioService.record (snap.getMxnBalance().add(received), btcLeft, price);
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
        BigDecimal unrealizedPnl = null;
        BigDecimal unrealizedPnlPct = null;
        if (t.getStatus() == TradeStatus.OPEN) {
            unrealizedPnl = Money.mxn(
                    t.getQuantity().multiply(currentPrice).subtract(t.getInvestedMxn()));
            unrealizedPnlPct = unrealizedPnl.multiply(HUNDRED).divide(
                    t.getInvestedMxn(), 4, RoundingMode.HALF_UP);
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
