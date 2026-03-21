package com.trading.bot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "signals_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "senal", length = 50, nullable = false)
    private String senal;

    @Column(nullable = false)
    private double precio;

    @Column(nullable = false)
    private int confianza;

    @Column(name = "score_buy")
    private Integer scoreBuy;

    @Column(name = "score_sell")
    private Integer scoreSell;

    @Column(name = "strategy_breakdown", columnDefinition = "TEXT")
    private String strategyBreakdown;
}
