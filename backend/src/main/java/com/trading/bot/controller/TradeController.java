package com.trading.bot.controller;

import com.trading.bot.dto.PortfolioDTO;
import com.trading.bot.dto.TradeDTO;
import com.trading.bot.service.MarketDataService;
import com.trading.bot.service.PortfolioService;
import com.trading.bot.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TradeController {

    private final TradeService tradeService;
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;

    public TradeController(TradeService tradeService,
                           PortfolioService portfolioService,
                           MarketDataService marketDataService) {
        this.tradeService      = tradeService;
        this.portfolioService  = portfolioService;
        this.marketDataService = marketDataService;
    }

    @GetMapping("/trades")
    public List<TradeDTO> getTrades() {
        return tradeService.getRecentTrades(marketDataService.currentPrice());
    }

    /** 204 si no hay operación abierta. */
    @GetMapping("/trades/open")
    public ResponseEntity<TradeDTO> getOpenTrade() {
        return tradeService.getOpenTrade(marketDataService.currentPrice())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/portfolio")
    public PortfolioDTO getPortfolio() {
        return portfolioService.getPortfolio(marketDataService.currentPrice());
    }
}
