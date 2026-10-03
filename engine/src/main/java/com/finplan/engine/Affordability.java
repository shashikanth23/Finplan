package com.finplan.engine;

import java.math.BigDecimal;

/**
 * @param bySurplus  what is left after expenses, existing EMIs and the savings target
 * @param byFoir     headroom under the lender rule of thumb: total EMIs at most 40% of take-home
 * @param maxNewEmi  the smaller of the two, never below zero
 * @param limitedBy  "SURPLUS" or "FOIR"
 */
public record Affordability(BigDecimal bySurplus, BigDecimal byFoir, BigDecimal maxNewEmi, String limitedBy) {

    public static final BigDecimal FOIR_LIMIT = new BigDecimal("0.40");

    public static Affordability compute(BigDecimal net, BigDecimal expenses, BigDecimal existingEmis, BigDecimal savings) {
        BigDecimal surplus = net.subtract(expenses).subtract(existingEmis).subtract(savings);
        BigDecimal foir = net.multiply(FOIR_LIMIT).subtract(existingEmis);
        boolean surplusBinds = surplus.compareTo(foir) <= 0;
        BigDecimal max = (surplusBinds ? surplus : foir).max(BigDecimal.ZERO);
        return new Affordability(Money.r2(surplus), Money.r2(foir), Money.r2(max), surplusBinds ? "SURPLUS" : "FOIR");
    }
}
