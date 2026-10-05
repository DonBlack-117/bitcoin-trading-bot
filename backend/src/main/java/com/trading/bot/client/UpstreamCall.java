package com.trading.bot.client;

import com.trading.bot.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Llamada a una API externa con hasta 3 intentos ante 429, 5xx o fallas de red.
 * Cualquier otro error se convierte en UpstreamException con el nombre de la fuente.
 */
public class UpstreamCall {

    private static final Logger log = LoggerFactory.getLogger(UpstreamCall.class);
    private static final int MAX_ATTEMPTS = 3;

    private final String source;
    private final Duration baseDelay;

    public UpstreamCall(String source, Duration baseDelay) {
        this.source = source;
        this.baseDelay = baseDelay;
    }

    public <T> T execute(Supplier<T> call) {
        for (int attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException | ResourceAccessException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new UpstreamException(source, describe(e), e);
                }
                log.warn("{} falló (intento {}/{}): {}", source, attempt, MAX_ATTEMPTS, describe(e));
                sleep(baseDelay.multipliedBy(1L << (attempt - 1)));
            } catch (RestClientResponseException e) {
                throw new UpstreamException(source, describe(e), e);
            } catch (UpstreamException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new UpstreamException(source, "respuesta inválida (" + e.getMessage() + ")", e);
            }
        }
    }

    private static String describe(Exception e) {
        if (e instanceof RestClientResponseException r) {
            return "HTTP " + r.getStatusCode().value();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new UpstreamException(source, "interrumpido");
        }
    }
}
