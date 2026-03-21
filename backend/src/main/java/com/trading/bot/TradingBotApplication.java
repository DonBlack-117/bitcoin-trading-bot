package com.trading.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TradingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingBotApplication.class, args);
    }
}
