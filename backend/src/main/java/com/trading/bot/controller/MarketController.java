package com.trading.bot.controller;

import com.trading.bot.client.BinanceClient;
import com.trading.bot.dto.ChartDTO;
import com.trading.bot.dto.GlobalMarketDTO;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.TickerDTO;
import com.trading.bot.service.CoinMarketCapService;
import com.trading.bot.service.MarketDataService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MarketController {

    private final MarketDataService marketDataService;
    private final CoinMarketCapService coinMarketCapService;

    public MarketController(MarketDataService marketDataService, CoinMarketCapService coinMarketCapService) {
        this.marketDataService = marketDataService;
        this.coinMarketCapService = coinMarketCapService;
    }

    @GetMapping("/ticker")
    public TickerDTO getTicker() {
        return marketDataService.getTicker();
    }

    @GetMapping("/ohlcv")
    public List<OhlcvCandleDTO> getOhlcv(
            @RequestParam(defaultValue = "120")
            @Min(value = 1, message = "limit debe ser al menos 1")
            @Max(value = BinanceClient.MAX_LIMIT, message = "limit no puede pasar de 1000") int limit) {
        return marketDataService.getCandlesMxn(limit);
    }

    /** Velas con RSI y bandas de Bollinger calculadas en el backend. */
    @GetMapping("/chart")
    public ChartDTO getChart(
            @RequestParam(defaultValue = "120")
            @Min(value = 30, message = "limit debe ser al menos 30")
            @Max(value = BinanceClient.MAX_LIMIT, message = "limit no puede pasar de 1000") int limit) {
        return marketDataService.getChart(limit);
    }

    @GetMapping("/market")
    public GlobalMarketDTO getMarket() {
        return coinMarketCapService.getGlobalMarket();
    }
}
