package com.trading.bot.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Redondeo de montos: pesos con 2 decimales y BTC con 8 (1 satoshi). */
public final class Money {

    public static final int MXN_SCALE = 2;
    public static final int BTC_SCALE = 8;

    private Money() {}

    public static BigDecimal mxn(double value) {
        return BigDecimal.valueOf(value).setScale(MXN_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal mxn(BigDecimal value) {
        return value.setScale(MXN_SCALE, RoundingMode.HALF_UP);
    }

    /** Las cantidades de BTC se redondean hacia abajo: nunca se compra más de lo que alcanza. */
    public static BigDecimal btc(BigDecimal value) {
        return value.setScale(BTC_SCALE, RoundingMode.DOWN);
    }
}
