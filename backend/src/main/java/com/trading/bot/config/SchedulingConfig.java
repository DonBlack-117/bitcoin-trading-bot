package com.trading.bot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Las pruebas apagan el scheduler con bot.scheduler.enabled=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "bot.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
