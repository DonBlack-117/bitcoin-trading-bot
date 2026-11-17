package com.trading.bot.exception;

import org.springframework.http.HttpStatus;

/** El upstream devolvió menos datos de los que necesita la estrategia (p. ej. pocas velas). */
public class InsufficientDataException extends ApiException {
    public InsufficientDataException(String message) {
        super("INSUFFICIENT_DATA", HttpStatus.SERVICE_UNAVAILABLE, message, null);
    }
}
