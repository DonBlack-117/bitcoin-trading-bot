package com.trading.bot.controller;

import com.trading.bot.dto.PortfolioDTO;
import com.trading.bot.dto.TradeDTO;
import com.trading.bot.service.BitsoService;
import com.trading.bot.service.PortfolioService;
import com.trading.bot.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TradeController {

    private final TradeService tradeService;
    private final PortfolioService portfolioService;
    private final BitsoService bitsoService;

    public TradeController(TradeService tradeService,
                           PortfolioService portfolioService,
                           BitsoService bitsoService) {
        this.tradeService    = tradeService;
        this.portfolioService = portfolioService;
        this.bitsoService    = bitsoService;
    }

    @GetMapping("/trades")
    public ResponseEntity<List<TradeDTO>> getTrades() {
        try {
            double price = bitsoService.getTicker().last();
            return ResponseEntity.ok(tradeService.getRecentTrades(price));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/trades/open")
    public ResponseEntity<TradeDTO> getOpenTrade() {
        try {
            double price = bitsoService.getTicker().last();
            Optional<TradeDTO> open = tradeService.getOpenTrade(price);
            return open.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/portfolio")
    public ResponseEntity<PortfolioDTO> getPortfolio() {
        try {
            double price = bitsoService.getTicker().last();
            return ResponseEntity.ok(portfolioService.getPortfolio(price));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
