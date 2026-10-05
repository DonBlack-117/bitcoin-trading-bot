package com.trading.bot.exception;

import org.springframework.http.HttpStatus;

public class ConfigMissingException extends ApiException {

    public ConfigMissingException(String message) {
        super("CONFIG_MISSING", HttpStatus.SERVICE_UNAVAILABLE, message, null);
    }
}
