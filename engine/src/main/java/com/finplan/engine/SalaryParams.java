package com.finplan.engine;

import java.math.BigDecimal;

/**
 * @param monthlyFixedDeductions      PF, professional tax and similar amounts deducted from pay each month
 * @param annualOldRegimeDeductions   80C/80D etc. claimed under the old regime (ignored for NEW)
 */
public record SalaryParams(TaxRegime regime, BigDecimal monthlyFixedDeductions, BigDecimal annualOldRegimeDeductions) {
    public SalaryParams {
        regime = regime == null ? TaxRegime.NEW : regime;
        monthlyFixedDeductions = Money.nz(monthlyFixedDeductions);
        annualOldRegimeDeductions = Money.nz(annualOldRegimeDeductions);
        Money.requireNonNegative(monthlyFixedDeductions, "monthlyFixedDeductions");
        Money.requireNonNegative(annualOldRegimeDeductions, "annualOldRegimeDeductions");
    }

    public static SalaryParams defaults() { return new SalaryParams(TaxRegime.NEW, BigDecimal.ZERO, BigDecimal.ZERO); }
}
