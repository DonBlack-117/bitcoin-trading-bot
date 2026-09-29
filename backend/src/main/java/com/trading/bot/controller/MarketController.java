package com.trading.bot.controller;

import com.trading.bot.dto.GlobalMarketDTO;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.TickerDTO;
import com.trading.bot.service.BitsoService;
import com.trading.bot.service.CoinMarketCapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MarketController {

    private static final Logger log = LoggerFactory.getLogger(MarketController.class);

    private final BitsoService bitsoService;
    private final CoinMarketCapService coinMarketCapService;

    public MarketController(BitsoService bitsoService, CoinMarketCapService coinMarketCapService) {
        this.bitsoService = bitsoService;
        this.coinMarketCapService = coinMarketCapService;
    }

    @GetMapping("/ticker")
    public ResponseEntity<TickerDTO> getTicker() {
        try {
            TickerDTO ticker = bitsoService.getTicker();
            return ResponseEntity.ok(ticker);
        } catch (Exception e) {
            log.error("GET /api/ticker failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/ohlcv")
    public ResponseEntity<List<OhlcvCandleDTO>> getOhlcv(
            @RequestParam(defaultValue = "120") int limit) {
        try {
            List<OhlcvCandleDTO> candles = bitsoService.getOhlcv(limit);
            return ResponseEntity.ok(candles);
        } catch (Exception e) {
            log.error("GET /api/ohlcv failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/market")
    public ResponseEntity<?> getMarket() {
        try {
            GlobalMarketDTO market = coinMarketCapService.getGlobalMarket();
            return ResponseEntity.ok(market);
        } catch (IllegalStateException e) {
            log.warn("GET /api/market: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (Exception e) {
            log.error("GET /api/market failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
