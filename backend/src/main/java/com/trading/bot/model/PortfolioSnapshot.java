package com.trading.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Saldo del portafolio simulado después de cada movimiento. El último es el saldo actual. */
@Entity
@Table(name = "portfolio_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mxn_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal mxnBalance;

    @Column(name = "btc_balance", nullable = false, precision = 19, scale = 8)
    private BigDecimal btcBalance;

    @Column(name = "btc_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal btcPrice;

    @Column(name = "total_value_mxn", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalValueMxn;

    @Column(name = "unrealized_pnl", precision = 19, scale = 2)
    private BigDecimal unrealizedPnl;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;
}
