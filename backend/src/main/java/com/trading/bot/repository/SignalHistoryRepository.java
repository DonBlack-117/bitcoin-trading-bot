package com.trading.bot.repository;

import com.trading.bot.model.SignalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SignalHistoryRepository extends JpaRepository<SignalHistory, Long> {

    List<SignalHistory> findTop8ByOrderByTimestampDesc();

    Optional<SignalHistory> findTopByOrderByTimestampDesc();
}
