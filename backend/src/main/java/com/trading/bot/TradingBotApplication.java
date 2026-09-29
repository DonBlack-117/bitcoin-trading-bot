package com.trading.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class TradingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingBotApplication.class, args);
    }
}
