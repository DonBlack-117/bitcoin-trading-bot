package com.trading.bot.model;

import com.trading.bot.domain.SignalType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cambios de señal: solo se guarda una fila cuando la señal es distinta de la anterior. */
@Entity
@Table(name = "signals_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SignalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "senal", length = 50, nullable = false)
    private SignalType senal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private int confianza;

    @Column(name = "score_buy")
    private Integer scoreBuy;

    @Column(name = "score_sell")
    private Integer scoreSell;

    @Column(name = "strategy_breakdown", columnDefinition = "TEXT")
    private String strategyBreakdown;
}
