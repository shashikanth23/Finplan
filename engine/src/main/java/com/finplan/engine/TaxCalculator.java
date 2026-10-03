package com.finplan.engine;

import java.math.BigDecimal;

/**
 * Indian income tax for a resident salaried individual under 60, FY 2025-26 / FY 2026-27 rules.
 * Slabs are plain data at the top of the class so a Budget change is a one-place edit.
 * Not modelled: surcharge (only matters above Rs 50 lakh), HRA/LTA exemptions, senior-citizen slabs.
 */
public final class TaxCalculator {

    // Slab upper bounds (inclusive) and the rate applied to the slice below that bound.
    private static final long[] NEW_LIMITS = {400_000, 800_000, 1_200_000, 1_600_000, 2_000_000, 2_400_000, Long.MAX_VALUE};
    private static final int[] NEW_RATES = {0, 5, 10, 15, 20, 25, 30};
    private static final long[] OLD_LIMITS = {250_000, 500_000, 1_000_000, Long.MAX_VALUE};
    private static final int[] OLD_RATES = {0, 5, 20, 30};

    private static final BigDecimal NEW_STANDARD_DEDUCTION = Money.of(75_000);
    private static final BigDecimal OLD_STANDARD_DEDUCTION = Money.of(50_000);
    private static final BigDecimal NEW_REBATE_INCOME_LIMIT = Money.of(1_200_000);
    private static final BigDecimal OLD_REBATE_INCOME_LIMIT = Money.of(500_000);
    private static final BigDecimal OLD_REBATE_MAX = Money.of(12_500);
    private static final BigDecimal CESS_RATE = new BigDecimal("0.04");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * @param oldRegimeDeductions Chapter VI-A deductions (80C, 80D, ...) already capped by the caller;
     *                            ignored under the new regime.
     */
    public TaxResult annualTax(BigDecimal annualGross, TaxRegime regime, BigDecimal oldRegimeDeductions) {
        Money.requireNonNegative(annualGross, "annualGross");
        BigDecimal gross = Money.nz(annualGross);
        TaxRegime r = regime == null ? TaxRegime.NEW : regime;

        BigDecimal standard = r == TaxRegime.NEW ? NEW_STANDARD_DEDUCTION : OLD_STANDARD_DEDUCTION;
        BigDecimal extra = r == TaxRegime.OLD ? Money.nz(oldRegimeDeductions) : BigDecimal.ZERO;
        BigDecimal taxable = gross.subtract(standard).subtract(extra).max(BigDecimal.ZERO);

        BigDecimal tax;
        if (r == TaxRegime.NEW) {
            tax = slabTax(taxable, NEW_LIMITS, NEW_RATES);
            if (taxable.compareTo(NEW_REBATE_INCOME_LIMIT) <= 0) {
                tax = BigDecimal.ZERO;                                    // 87A: full rebate up to 12 lakh
            } else {
                tax = tax.min(taxable.subtract(NEW_REBATE_INCOME_LIMIT)); // marginal relief just above 12 lakh
            }
        } else {
            tax = slabTax(taxable, OLD_LIMITS, OLD_RATES);
            if (taxable.compareTo(OLD_REBATE_INCOME_LIMIT) <= 0) {
                tax = tax.subtract(tax.min(OLD_REBATE_MAX));
            }
        }

        BigDecimal cess = Money.r2(tax.multiply(CESS_RATE));
        BigDecimal total = Money.r2(tax.add(cess));
        return new TaxResult(Money.r2(gross), Money.r2(taxable), Money.r2(tax), cess, total,
                Money.pct(total, gross));
    }

    private static BigDecimal slabTax(BigDecimal taxable, long[] limits, int[] rates) {
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal prev = BigDecimal.ZERO;
        for (int i = 0; i < limits.length; i++) {
            if (taxable.compareTo(prev) <= 0) break;
            BigDecimal top = taxable.min(BigDecimal.valueOf(limits[i]));
            tax = tax.add(top.subtract(prev).multiply(BigDecimal.valueOf(rates[i])).divide(HUNDRED));
            prev = BigDecimal.valueOf(limits[i]);
        }
        return tax;
    }
}
