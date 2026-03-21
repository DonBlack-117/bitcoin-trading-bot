package com.trading.bot.controller;

import com.trading.bot.dto.AnalysisRequestDTO;
import com.trading.bot.dto.AnalysisResponseDTO;
import com.trading.bot.dto.CryptoDTO;
import com.trading.bot.service.AnalysisService;
import com.trading.bot.service.CoinMarketCapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService      analysisService;
    private final CoinMarketCapService cmcService;

    public AnalysisController(AnalysisService analysisService,
                               CoinMarketCapService cmcService) {
        this.analysisService = analysisService;
        this.cmcService      = cmcService;
    }

    /**
     * Returns the top 100 crypto list (symbol + name + rank) for the selector dropdown.
     */
    @GetMapping("/cryptos")
    public ResponseEntity<List<CryptoDTO>> getCryptoList() {
        try {
            List<CryptoDTO> list = cmcService.getCryptoList(100, "MXN");
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Analyzes a cryptocurrency for a given MXN amount and returns a trading recommendation.
     * Body: { "symbol": "ETH", "amountMxn": 1000 }
     */
    @PostMapping
    public ResponseEntity<?> analyze(@RequestBody AnalysisRequestDTO request) {
        try {
            AnalysisResponseDTO result = analysisService.analyze(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al analizar: " + e.getMessage());
        }
    }
}
