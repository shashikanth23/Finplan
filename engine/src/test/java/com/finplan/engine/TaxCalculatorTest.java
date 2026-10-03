package com.finplan.engine;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxCalculatorTest {
    private final TaxCalculator calc = new TaxCalculator();

    private static BigDecimal n(String v) { return new BigDecimal(v); }

    @Test void newRegimeIsTaxFreeUpTo12LakhTaxable() {
        // 12.75 lakh gross - 75,000 standard deduction = 12 lakh taxable, fully rebated
        assertEquals(0, calc.annualTax(n("1275000"), TaxRegime.NEW, null).totalTax().signum());
    }

    @Test void newRegimeFifteenLakh() {
        // taxable 14.25L: 20,000 + 40,000 + 33,750 = 93,750, plus 4% cess = 97,500
        assertEquals(0, n("97500.00").compareTo(calc.annualTax(n("1500000"), TaxRegime.NEW, null).totalTax()));
    }

    @Test void marginalReliefJustAboveRebateLimit() {
        // taxable 12.1L would be 61,500 by slabs, but tax is capped at the 10,000 by which income exceeds 12L
        TaxResult r = calc.annualTax(n("1285000"), TaxRegime.NEW, null);
        assertEquals(0, n("10000.00").compareTo(r.incomeTax()));
    }

    @Test void oldRegimeWithDeductions() {
        // 10L - 50,000 - 1.5L = 8L taxable: 12,500 + 60,000 = 72,500 + 4% cess = 75,400
        assertEquals(0, n("75400.00").compareTo(calc.annualTax(n("1000000"), TaxRegime.OLD, n("150000")).totalTax()));
    }

    @Test void oldRegimeRebateUpTo5LakhTaxable() {
        assertEquals(0, calc.annualTax(n("550000"), TaxRegime.OLD, null).totalTax().signum());
    }
}
