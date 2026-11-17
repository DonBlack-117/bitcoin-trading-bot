package com.trading.bot.exception;

import org.springframework.http.HttpStatus;

/** La última señal es demasiado vieja: el scheduler lleva varios ciclos sin calcular una nueva. */
public class SignalStaleException extends ApiException {
    public SignalStaleException(long ageSeconds) {
        super("SIGNAL_STALE", HttpStatus.SERVICE_UNAVAILABLE,
                "La última señal tiene " + ageSeconds + " s y ya no es confiable; "
                        + "el cálculo de la señal podría estar caído", null);
    }
}
