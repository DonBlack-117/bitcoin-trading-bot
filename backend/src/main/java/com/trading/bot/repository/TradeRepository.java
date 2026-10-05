package com.trading.bot.repository;

import com.trading.bot.domain.TradeStatus;
import com.trading.bot.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    Optional<Trade> findTopByStatusOrderByOpenedAtDesc(TradeStatus status);

    List<Trade> findTop20ByOrderByOpenedAtDesc();

    long countByStatus(TradeStatus status);

    long countByStatusAndProfitLossGreaterThan(TradeStatus status, BigDecimal profitLoss);
}
