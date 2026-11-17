package com.trading.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj inyectable: las pruebas usan uno fijo. */
@Configuration
public class ClockConfig {
    @Bean
    Clock clock() {
        // UTC: las marcas de tiempo no dependen de la zona del servidor ni del horario de verano,
        // así que ORDER BY opened_at/timestamp queda estable.
        return Clock.systemUTC();
    }
}
