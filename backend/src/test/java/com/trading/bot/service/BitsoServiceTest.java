package com.trading.bot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BitsoServiceTest {

    @Test
    void percentChangeUsesThePriceFrom24hAgoAsBase() {
        // Bitso: last=1,496,570 y change_24=17,300 MXN → abrió en 1,479,270
        assertEquals(1.1695, BitsoService.percentChange(1_496_570, 17_300), 1e-4);
        assertEquals(-10.0, BitsoService.percentChange(90, -10), 1e-9);
        assertEquals(0.0, BitsoService.percentChange(100, 0), 1e-9);
    }

    @Test
    void percentChangeIsZeroWhenThereIsNoValidBase() {
        assertEquals(0.0, BitsoService.percentChange(100, 100), 1e-9);
        assertEquals(0.0, BitsoService.percentChange(0, 0), 1e-9);
    }
}
