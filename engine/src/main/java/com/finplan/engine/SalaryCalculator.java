package com.finplan.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Gross to net, and the reverse: "what gross salary do I need to take home X?" */
public final class SalaryCalculator {

    /** Grid size for the search below. Real tax rules make take-home slightly non-monotonic, see requiredGross. */
    private static final long STEP = 100;

    private final TaxCalculator tax = new TaxCalculator();

    public NetSalary net(BigDecimal monthlyGross, SalaryParams p) {
        Money.requireNonNegative(monthlyGross, "monthlyGross");
        BigDecimal gross = Money.nz(monthlyGross);
        BigDecimal annualTax = tax.annualTax(gross.multiply(BigDecimal.valueOf(12)), p.regime(),
                p.annualOldRegimeDeductions()).totalTax();
        BigDecimal monthlyTax = Money.r2(annualTax.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP));
        BigDecimal net = gross.subtract(monthlyTax).subtract(p.monthlyFixedDeductions());
        return new NetSalary(Money.r2(gross), monthlyTax, Money.r2(p.monthlyFixedDeductions()), Money.r2(net));
    }

    /**
     * Smallest whole-rupee monthly gross whose take-home is at least {@code targetNet}.
     *
     * <p>Plain binary search is NOT safe here, because take-home is not monotonic in gross pay:
     * <ul>
     *   <li>New regime: just above the 12 lakh rebate limit, marginal relief taxes each extra rupee at 100%,
     *       plus 4% cess, so take-home dips slightly across roughly Rs 6,000 of monthly gross.</li>
     *   <li>Old regime: the 87A rebate ends at a cliff, so take-home drops by about Rs 13,000 at that point.</li>
     * </ul>
     * So we scan a coarse grid upward from the theoretical minimum (target + fixed deductions, since tax
     * cannot be negative), and once a grid point qualifies, scan the gap below it rupee by rupee.
     * A qualifying window narrower than {@value #STEP} rupees right before an old-regime cliff could be
     * missed, in which case the answer is higher than the true minimum but still meets the target.
     */
    public BigDecimal requiredGross(BigDecimal targetNet, SalaryParams p) {
        Money.requireNonNegative(targetNet, "targetNet");
        BigDecimal target = Money.nz(targetNet);
        if (target.signum() == 0) return BigDecimal.ZERO;

        long lo = target.setScale(0, RoundingMode.CEILING).longValueExact()
                + p.monthlyFixedDeductions().setScale(0, RoundingMode.CEILING).longValueExact();

        long hi = lo;                            // exponential search for any gross that reaches the target
        int guard = 0;
        while (net(hi, p).compareTo(target) < 0) {
            hi *= 2;
            if (++guard > 40) throw new IllegalArgumentException("Target take-home is not reachable");
        }

        long prev = lo - 1;
        for (long g = lo; ; g = Math.min(g + STEP, hi)) {
            if (net(g, p).compareTo(target) >= 0) {
                for (long f = prev + 1; f <= g; f++) {
                    if (net(f, p).compareTo(target) >= 0) return BigDecimal.valueOf(f);
                }
            }
            if (g == hi) break;
            prev = g;
        }
        return BigDecimal.valueOf(hi);
    }

    private BigDecimal net(long gross, SalaryParams p) {
        return net(BigDecimal.valueOf(gross), p).monthlyNet();
    }
}
