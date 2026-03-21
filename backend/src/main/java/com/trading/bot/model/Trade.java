package com.trading.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "trade_type", nullable = false, length = 10)
    private String tradeType;           // "BUY"

    @Column(name = "entry_price", nullable = false)
    private Double entryPrice;

    @Column(name = "exit_price")
    private Double exitPrice;

    @Column(nullable = false)
    private Double quantity;            // BTC amount

    @Column(name = "invested_mxn", nullable = false)
    private Double investedMxn;         // MXN invested

    @Column(name = "stop_loss")
    private Double stopLoss;

    @Column(name = "take_profit")
    private Double takeProfit;

    @Column(nullable = false, length = 20)
    private String status;              // "OPEN", "CLOSED"

    @Column(name = "close_reason", length = 20)
    private String closeReason;         // "SIGNAL", "STOP_LOSS", "TAKE_PROFIT"

    @Column(name = "profit_loss")
    private Double profitLoss;

    @Column(name = "profit_loss_pct")
    private Double profitLossPct;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "entry_signal_id")
    private Long entrySignalId;

    @Column(name = "exit_signal_id")
    private Long exitSignalId;
}
