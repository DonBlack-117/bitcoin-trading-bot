package com.trading.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mxn_balance", nullable = false)
    private Double mxnBalance;

    @Column(name = "btc_balance", nullable = false)
    private Double btcBalance;

    @Column(name = "btc_price", nullable = false)
    private Double btcPrice;

    @Column(name = "total_value_mxn", nullable = false)
    private Double totalValueMxn;

    @Column(name = "unrealized_pnl")
    private Double unrealizedPnl;

    @Column(name = "snapshot_at")
    private LocalDateTime snapshotAt;

    @PrePersist
    public void prePersist() {
        if (snapshotAt == null) snapshotAt = LocalDateTime.now();
    }
}
