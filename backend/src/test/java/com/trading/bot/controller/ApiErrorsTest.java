package com.trading.bot.controller;

import com.trading.bot.dto.TickerDTO;
import com.trading.bot.exception.ConfigMissingException;
import com.trading.bot.exception.NotFoundException;
import com.trading.bot.exception.SignalNotReadyException;
import com.trading.bot.exception.UpstreamException;
import com.trading.bot.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Todos los errores salen como {"error": {"code", "message"}} con el estado HTTP correcto. */
@WebMvcTest
class ApiErrorsTest {

    @Autowired MockMvc mvc;

    @MockitoBean MarketDataService marketDataService;
    @MockitoBean CoinMarketCapService coinMarketCapService;
    @MockitoBean SignalScheduler signalScheduler;
    @MockitoBean SignalHistoryService signalHistoryService;
    @MockitoBean TradeService tradeService;
    @MockitoBean PortfolioService portfolioService;
    @MockitoBean AnalysisService analysisService;

    @Test
    void chartLimitAboveBinanceMaximumIsAValidationError() throws Exception {
        mvc.perform(get("/api/chart").param("limit", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("limit no puede pasar de 1000"));
    }

    @Test
    void nonNumericLimitIsAValidationError() throws Exception {
        mvc.perform(get("/api/ohlcv").param("limit", "muchas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void analysisWithoutSymbolIsAValidationErrorNotA500() throws Exception {
        mvc.perform(post("/api/analysis").contentType(MediaType.APPLICATION_JSON).content("{\"amountMxn\": 100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Elige una criptomoneda"));
    }

    @Test
    void analysisWithNegativeAmountIsAValidationError() throws Exception {
        mvc.perform(post("/api/analysis").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\": \"ETH\", \"amountMxn\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("El monto debe ser mayor a 0"));
    }

    @Test
    void malformedJsonIsAValidationError() throws Exception {
        mvc.perform(post("/api/analysis").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownCryptoIs404() throws Exception {
        when(analysisService.analyze(any())).thenThrow(new NotFoundException("CRYPTO_NOT_FOUND", "no está"));

        mvc.perform(post("/api/analysis").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\": \"ZZZ\", \"amountMxn\": 5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CRYPTO_NOT_FOUND"));
    }

    @Test
    void signalBeforeTheFirstRunIs503() throws Exception {
        when(signalScheduler.getLatest()).thenThrow(new SignalNotReadyException());

        mvc.perform(get("/api/signal"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SIGNAL_NOT_READY"));
    }

    @Test
    void externalApiFailureIs502() throws Exception {
        when(marketDataService.getTicker()).thenThrow(new UpstreamException("Bitso", "HTTP 500"));

        mvc.perform(get("/api/ticker"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Bitso: HTTP 500"));
    }

    @Test
    void missingCmcKeyIs503() throws Exception {
        when(coinMarketCapService.getGlobalMarket()).thenThrow(new ConfigMissingException("Falta la variable de entorno CMC_API_KEY"));

        mvc.perform(get("/api/market"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("CONFIG_MISSING"));
    }

    @Test
    void unexpectedErrorDoesNotLeakDetails() throws Exception {
        when(marketDataService.getTicker()).thenThrow(new IllegalStateException("detalle interno"));

        mvc.perform(get("/api/ticker"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Error interno del servidor"));
    }

    @Test
    void noOpenTradeIs204() throws Exception {
        when(marketDataService.currentPrice()).thenReturn(1_500_000.0);
        when(tradeService.getOpenTrade(anyDouble())).thenReturn(Optional.empty());

        mvc.perform(get("/api/trades/open")).andExpect(status().isNoContent());
    }

    @Test
    void tickerIsReturnedAsJson() throws Exception {
        when(marketDataService.getTicker()).thenReturn(new TickerDTO(1_500_000, 1_500_100, 1_499_900, 12.5, 2.04));

        mvc.perform(get("/api/ticker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last").value(1_500_000.0));
    }
}
