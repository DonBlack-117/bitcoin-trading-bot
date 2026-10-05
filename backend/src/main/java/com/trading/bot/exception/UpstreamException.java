package com.trading.bot.exception;

import org.springframework.http.HttpStatus;

/** Bitso, Binance o CoinMarketCap fallaron o respondieron algo que no se puede usar. */
public class UpstreamException extends ApiException {

    public UpstreamException(String source, String message) {
        this(source, message, null);
    }

    public UpstreamException(String source, String message, Throwable cause) {
        super("UPSTREAM_ERROR", HttpStatus.BAD_GATEWAY, source + ": " + message, cause);
    }
}
