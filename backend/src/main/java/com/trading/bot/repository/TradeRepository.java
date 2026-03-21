package com.trading.bot.repository;

import com.trading.bot.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    Optional<Trade> findTopByStatusOrderByOpenedAtDesc(String status);

    List<Trade> findTop20ByOrderByOpenedAtDesc();

    long countByStatus(String status);

    long countByStatusAndProfitLossGreaterThan(String status, Double profitLoss);
}
