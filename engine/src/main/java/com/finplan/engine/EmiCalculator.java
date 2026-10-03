package com.finplan.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Reducing-balance loan maths: EMI = P*r*(1+r)^n / ((1+r)^n - 1), r = annual% / 12 / 100. */
public final class EmiCalculator {

    private static final MathContext MC = new MathContext(24, RoundingMode.HALF_EVEN);
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    private EmiCalculator() {}

    public static BigDecimal emi(BigDecimal principal, BigDecimal annualRatePct, int months) {
        validate(principal, annualRatePct, months);
        if (annualRatePct.signum() == 0) return Money.r2(principal.divide(BigDecimal.valueOf(months), MC));
        BigDecimal r = monthlyRate(annualRatePct);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(months, MC);
        return Money.r2(principal.multiply(r, MC).multiply(pow, MC).divide(pow.subtract(BigDecimal.ONE), MC));
    }

    /** The loan you can take for a given EMI (inverse of {@link #emi}). */
    public static BigDecimal affordablePrincipal(BigDecimal emi, BigDecimal annualRatePct, int months) {
        validate(emi, annualRatePct, months);
        if (annualRatePct.signum() == 0) return Money.r2(emi.multiply(BigDecimal.valueOf(months)));
        BigDecimal r = monthlyRate(annualRatePct);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(months, MC);
        return Money.r2(emi.multiply(pow.subtract(BigDecimal.ONE), MC).divide(r.multiply(pow, MC), MC));
    }

    /** Outstanding principal after {@code paidMonths} on-time EMIs. */
    public static BigDecimal remainingBalance(BigDecimal principal, BigDecimal annualRatePct, int months, int paidMonths) {
        validate(principal, annualRatePct, months);
        int paid = Math.max(0, Math.min(paidMonths, months));
        if (paid == months) return Money.r2(BigDecimal.ZERO);
        if (annualRatePct.signum() == 0) {
            return Money.r2(principal.multiply(BigDecimal.valueOf(months - paid)).divide(BigDecimal.valueOf(months), MC));
        }
        BigDecimal opr = BigDecimal.ONE.add(monthlyRate(annualRatePct));
        BigDecimal powN = opr.pow(months, MC);
        BigDecimal powP = opr.pow(paid, MC);
        return Money.r2(principal.multiply(powN.subtract(powP), MC).divide(powN.subtract(BigDecimal.ONE), MC));
    }

    public static List<ScheduleRow> schedule(BigDecimal principal, BigDecimal annualRatePct, int months) {
        BigDecimal emi = emi(principal, annualRatePct, months);
        BigDecimal r = annualRatePct.signum() == 0 ? BigDecimal.ZERO : monthlyRate(annualRatePct);
        List<ScheduleRow> rows = new ArrayList<>(months);
        BigDecimal balance = Money.r2(principal);
        for (int m = 1; m <= months; m++) {
            BigDecimal interest = Money.r2(balance.multiply(r, MC));
            BigDecimal principalPart = emi.subtract(interest);
            if (m == months || principalPart.compareTo(balance) > 0) principalPart = balance; // absorb rounding drift
            balance = balance.subtract(principalPart);
            rows.add(new ScheduleRow(m, principalPart.add(interest), interest, principalPart, balance));
        }
        return rows;
    }

    private static BigDecimal monthlyRate(BigDecimal annualRatePct) {
        return annualRatePct.divide(TWELVE_HUNDRED, MC);
    }

    private static void validate(BigDecimal amount, BigDecimal rate, int months) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (rate == null || rate.signum() < 0) throw new IllegalArgumentException("Interest rate must not be negative");
        if (months <= 0 || months > 600) throw new IllegalArgumentException("Tenure must be between 1 and 600 months");
    }
}
