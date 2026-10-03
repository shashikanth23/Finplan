package com.finplan.engine;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SalaryCalculatorTest {
    private final SalaryCalculator calc = new SalaryCalculator();

    @Test void requiredGrossMatchesProductExample() {
        // Expenses 35k + EMIs 20k + savings 15k = 70k take-home; 10k of PF etc. deducted monthly.
        // 80k gross is 9.6L a year, which is inside the new-regime tax-free zone, so gross = 70k + 10k.
        SalaryParams p = new SalaryParams(TaxRegime.NEW, new BigDecimal("10000"), BigDecimal.ZERO);
        assertEquals(0, new BigDecimal("80000").compareTo(calc.requiredGross(new BigDecimal("70000"), p)));
    }

    @Test void requiredGrossIsSmallestSufficientAndRoundTrips() {
        SalaryParams p = SalaryParams.defaults();
        BigDecimal target = new BigDecimal("150000");
        BigDecimal gross = calc.requiredGross(target, p);
        assertTrue(calc.net(gross, p).monthlyNet().compareTo(target) >= 0);
        assertTrue(calc.net(gross.subtract(BigDecimal.ONE), p).monthlyNet().compareTo(target) < 0);
    }

    @Test void takeHomeDipsInsideMarginalReliefBand() {
        // Documented behaviour, not a bug: just above the 12L rebate limit each extra rupee is taxed at 104%.
        SalaryParams p = SalaryParams.defaults();
        BigDecimal atLimit = calc.net(new BigDecimal("106250"), p).monthlyNet();
        BigDecimal inside = calc.net(new BigDecimal("109000"), p).monthlyNet();
        assertTrue(inside.compareTo(atLimit) < 0);
    }

    @Test void requiredGrossIsMinimalForTargetsInsideTheDipAndAcrossTheOldRegimeCliff() {
        SalaryParams newRegime = SalaryParams.defaults();
        SalaryParams oldRegime = new SalaryParams(TaxRegime.OLD, new BigDecimal("5000"), new BigDecimal("200000"));
        long[] targets = {70_000, 105_000, 106_000, 106_250, 108_000, 111_000, 150_000};
        for (long t : targets) assertMinimal(newRegime, BigDecimal.valueOf(t));
        for (long t : new long[]{50_000, 55_000, 58_000, 60_000, 70_000}) assertMinimal(oldRegime, BigDecimal.valueOf(t));
    }

    private void assertMinimal(SalaryParams p, BigDecimal target) {
        BigDecimal r = calc.requiredGross(target, p);
        assertTrue(calc.net(r, p).monthlyNet().compareTo(target) >= 0, "target not met for " + target);
        assertTrue(calc.net(r.subtract(BigDecimal.ONE), p).monthlyNet().compareTo(target) < 0, "not minimal for " + target);
    }
}
