package com.trading.bot.controller;

import com.trading.bot.dto.AnalysisRequestDTO;
import com.trading.bot.dto.AnalysisResponseDTO;
import com.trading.bot.dto.CryptoDTO;
import com.trading.bot.service.AnalysisService;
import com.trading.bot.service.CoinMarketCapService;
import jakarta.validation.Valid;
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

    /** Top 100 (símbolo, nombre y rango) para el buscador del asistente. */
    @GetMapping("/cryptos")
    public List<CryptoDTO> getCryptoList() {
        return cmcService.getCryptoList(100, "MXN");
    }

    /**
     * Recomendación para invertir un monto en pesos en una criptomoneda.
     * Body: { "symbol": "ETH", "amountMxn": 1000 }
     */
    @PostMapping
    public AnalysisResponseDTO analyze(@Valid @RequestBody AnalysisRequestDTO request) {
        return analysisService.analyze(request);
    }
}
