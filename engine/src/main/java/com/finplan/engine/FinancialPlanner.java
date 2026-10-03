package com.finplan.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Ties tax, salary, EMI affordability and health scoring into one summary, and runs what-if scenarios. */
public final class FinancialPlanner {

    private final SalaryCalculator salaryCalc = new SalaryCalculator();

    public FinancialSummary summarize(FinancialInput in) {
        NetSalary net = salaryCalc.net(in.monthlyGross(), in.salary());
        BigDecimal takeHome = net.monthlyNet();
        BigDecimal disposable = takeHome.subtract(in.monthlyExpenses()).subtract(in.monthlyEmis());
        BigDecimal freeCash = disposable.subtract(in.monthlySavingsTarget());

        BigDecimal needed = in.monthlyExpenses().add(in.monthlyEmis()).add(in.monthlySavingsTarget());
        BigDecimal requiredGross = salaryCalc.requiredGross(needed, in.salary());

        return new FinancialSummary(
                net.monthlyGross(), net.monthlyTax(), takeHome,
                Money.r2(in.monthlyExpenses()), Money.r2(in.monthlyEmis()), Money.r2(in.monthlySavingsTarget()),
                Money.r2(disposable), Money.r2(freeCash),
                requiredGross, requiredGross.subtract(net.monthlyGross()),
                Money.pct(in.monthlyEmis(), takeHome), Money.pct(in.monthlySavingsTarget(), takeHome),
                Affordability.compute(takeHome, in.monthlyExpenses(), in.monthlyEmis(), in.monthlySavingsTarget()),
                HealthScore.compute(takeHome, in.monthlyExpenses(), in.monthlyEmis(), in.monthlySavingsTarget()));
    }

    public ScenarioResult simulate(FinancialInput base, Scenario s) {
        BigDecimal gross = applyPct(base.monthlyGross(), s.grossChangePct());
        BigDecimal expenses = applyPct(base.monthlyExpenses(), s.expenseChangePct());
        BigDecimal emis = base.monthlyEmis().add(Money.nz(s.extraMonthlyEmi()));
        BigDecimal savings = s.newSavingsTarget() != null ? s.newSavingsTarget() : base.monthlySavingsTarget();

        FinancialSummary before = summarize(base);
        FinancialSummary after = summarize(new FinancialInput(gross, base.salary(), expenses, emis, savings));
        return new ScenarioResult(before, after,
                after.monthlyNet().subtract(before.monthlyNet()),
                after.freeCashAfterSavings().subtract(before.freeCashAfterSavings()),
                after.health().score() - before.health().score());
    }

    private static BigDecimal applyPct(BigDecimal value, BigDecimal pct) {
        if (pct == null) return value;
        BigDecimal factor = BigDecimal.ONE.add(pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return value.multiply(factor).max(BigDecimal.ZERO);
    }
}
