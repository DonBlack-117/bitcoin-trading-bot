package com.trading.bot.repository;

import com.trading.bot.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    Optional<PortfolioSnapshot> findTopByOrderBySnapshotAtDesc();
}
