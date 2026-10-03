package com.finplan.engine;

import java.math.BigDecimal;

public record FinancialInput(BigDecimal monthlyGross, SalaryParams salary, BigDecimal monthlyExpenses,
                             BigDecimal monthlyEmis, BigDecimal monthlySavingsTarget) {
    public FinancialInput {
        monthlyGross = Money.nz(monthlyGross);
        salary = salary == null ? SalaryParams.defaults() : salary;
        monthlyExpenses = Money.nz(monthlyExpenses);
        monthlyEmis = Money.nz(monthlyEmis);
        monthlySavingsTarget = Money.nz(monthlySavingsTarget);
        Money.requireNonNegative(monthlyGross, "monthlyGross");
        Money.requireNonNegative(monthlyExpenses, "monthlyExpenses");
        Money.requireNonNegative(monthlyEmis, "monthlyEmis");
        Money.requireNonNegative(monthlySavingsTarget, "monthlySavingsTarget");
    }
}
