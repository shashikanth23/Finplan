package com.finplan.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Small helpers so every calculation rounds and null-handles money the same way. */
public final class Money {
    private Money() {}

    public static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    public static BigDecimal r2(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    public static BigDecimal of(long v) { return BigDecimal.valueOf(v); }

    /** part / whole as a percentage with 2 decimals; 0 when whole is 0. */
    public static BigDecimal pct(BigDecimal part, BigDecimal whole) {
        if (whole.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 2, RoundingMode.HALF_UP);
    }

    public static void requireNonNegative(BigDecimal v, String name) {
        if (v != null && v.signum() < 0) throw new IllegalArgumentException(name + " must not be negative");
    }
}
