package com.trading.bot.exception;

import org.springframework.http.HttpStatus;

/** El scheduler todavía no calcula la primera señal (los primeros segundos tras arrancar). */
public class SignalNotReadyException extends ApiException {

    public SignalNotReadyException() {
        super("SIGNAL_NOT_READY", HttpStatus.SERVICE_UNAVAILABLE,
                "La primera señal se está calculando; vuelve a intentar en unos segundos", null);
    }
}
