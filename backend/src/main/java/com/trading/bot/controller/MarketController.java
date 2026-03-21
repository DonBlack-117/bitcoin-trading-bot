package com.trading.bot.controller;

import com.trading.bot.dto.GlobalMarketDTO;
import com.trading.bot.dto.OhlcvCandleDTO;
import com.trading.bot.dto.TickerDTO;
import com.trading.bot.service.BitsoService;
import com.trading.bot.service.CoinMarketCapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MarketController {

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
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/market")
    public ResponseEntity<GlobalMarketDTO> getMarket() {
        try {
            GlobalMarketDTO market = coinMarketCapService.getGlobalMarket();
            return ResponseEntity.ok(market);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
