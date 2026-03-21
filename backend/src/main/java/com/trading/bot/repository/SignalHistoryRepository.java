package com.trading.bot.repository;

import com.trading.bot.model.SignalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalHistoryRepository extends JpaRepository<SignalHistory, Long> {

    List<SignalHistory> findTop8ByOrderByTimestampDesc();

    SignalHistory findTopByOrderByTimestampDesc();
}
