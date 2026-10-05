package com.trading.bot.service;

import com.trading.bot.config.BotProperties;
import com.trading.bot.domain.Money;
import com.trading.bot.domain.TradeStatus;
import com.trading.bot.dto.PortfolioDTO;
import com.trading.bot.model.PortfolioSnapshot;
import com.trading.bot.repository.PortfolioSnapshotRepository;
import com.trading.bot.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PortfolioService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PortfolioSnapshotRepository snapshotRepository;
    private final TradeRepository tradeRepository;
    private final BotProperties properties;
    private final Clock clock;

    public PortfolioService(PortfolioSnapshotRepository snapshotRepository,
                            TradeRepository tradeRepository,
                            BotProperties properties,
                            Clock clock) {
        this.snapshotRepository = snapshotRepository;
        this.tradeRepository    = tradeRepository;
        this.properties         = properties;
        this.clock              = clock;
    }

    /** Crea el portafolio inicial si todavía no existe y devuelve el saldo actual. */
    @Transactional
    public PortfolioSnapshot ensureInitialized(BigDecimal btcPrice) {
        return snapshotRepository.findTopByOrderBySnapshotAtDescIdDesc()
                .orElseGet(() -> record(properties.initialMxn(), BigDecimal.ZERO, btcPrice));
    }

    /** Guarda un nuevo saldo. Lo llama TradeService dentro de su transacción. */
    @Transactional
    public PortfolioSnapshot record(BigDecimal mxn, BigDecimal btc, BigDecimal btcPrice) {
        BigDecimal price = Money.mxn(btcPrice);
        return snapshotRepository.save(PortfolioSnapshot.builder()
                .mxnBalance(Money.mxn(mxn))
                .btcBalance(Money.btc(btc))
                .btcPrice(price)
                .totalValueMxn(Money.mxn(mxn.add(btc.multiply(price))))
                .unrealizedPnl(Money.mxn(BigDecimal.ZERO))
                .snapshotAt(LocalDateTime.now(clock))
                .build());
    }

    /**
     * Estado actual del portafolio, valuado al precio de BTC que se pasa. Solo lee: si el
     * scheduler todavía no crea el portafolio, se muestra el capital inicial sin guardarlo.
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolio(double currentBtcPrice) {
        BigDecimal price = Money.mxn(currentBtcPrice);
        PortfolioSnapshot snap = snapshotRepository.findTopByOrderBySnapshotAtDescIdDesc()
                .orElseGet(() -> PortfolioSnapshot.builder()
                        .mxnBalance(Money.mxn(properties.initialMxn()))
                        .btcBalance(Money.btc(BigDecimal.ZERO))
                        .build());

        // El BTC de la operación abierta ya está en btcBalance: no se suma otra vez su ganancia
        BigDecimal totalValue = Money.mxn(snap.getMxnBalance().add(snap.getBtcBalance().multiply(price)));

        BigDecimal unrealisedPnl = tradeRepository.findTopByStatusOrderByOpenedAtDesc(TradeStatus.OPEN)
                .map(t -> Money.mxn(t.getQuantity().multiply(price).subtract(t.getInvestedMxn())))
                .orElse(Money.mxn(BigDecimal.ZERO));

        BigDecimal initial     = properties.initialMxn();
        BigDecimal totalReturn = totalValue.subtract(initial);
        BigDecimal totalRetPct = totalReturn.multiply(HUNDRED).divide(initial, 2, RoundingMode.HALF_UP);

        long closedCount  = tradeRepository.countByStatus(TradeStatus.CLOSED);
        long winningCount = tradeRepository.countByStatusAndProfitLossGreaterThan(TradeStatus.CLOSED, BigDecimal.ZERO);
        BigDecimal winRate = closedCount > 0
                ? BigDecimal.valueOf(winningCount * 100).divide(BigDecimal.valueOf(closedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new PortfolioDTO(
                Money.mxn(initial),
                snap.getMxnBalance(),
                snap.getBtcBalance(),
                price,
                totalValue,
                unrealisedPnl,
                totalReturn,
                totalRetPct,
                (int) closedCount,
                (int) winningCount,
                winRate);
    }
}
