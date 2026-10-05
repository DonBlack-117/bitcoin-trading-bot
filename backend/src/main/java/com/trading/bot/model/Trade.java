package com.trading.bot.model;

import com.trading.bot.domain.CloseReason;
import com.trading.bot.domain.TradeStatus;
import com.trading.bot.domain.TradeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 10)
    private TradeType tradeType;

    @Column(name = "entry_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal entryPrice;

    @Column(name = "exit_price", precision = 19, scale = 2)
    private BigDecimal exitPrice;

    /** BTC comprados. */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "invested_mxn", nullable = false, precision = 19, scale = 2)
    private BigDecimal investedMxn;

    @Column(name = "stop_loss", precision = 19, scale = 2)
    private BigDecimal stopLoss;

    @Column(name = "take_profit", precision = 19, scale = 2)
    private BigDecimal takeProfit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason", length = 20)
    private CloseReason closeReason;

    @Column(name = "profit_loss", precision = 19, scale = 2)
    private BigDecimal profitLoss;

    @Column(name = "profit_loss_pct", precision = 9, scale = 4)
    private BigDecimal profitLossPct;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "entry_signal_id")
    private Long entrySignalId;

    @Column(name = "exit_signal_id")
    private Long exitSignalId;
}
