package com.finplan.engine;

import java.math.BigDecimal;

/**
 * @param disposableIncome     take-home minus expenses and EMIs (before setting money aside)
 * @param freeCashAfterSavings disposableIncome minus the savings target; negative means a monthly shortfall
 * @param requiredGross        gross salary needed to cover expenses + EMIs + savings target
 * @param grossGap             requiredGross minus current gross; positive means you need a raise
 */
public record FinancialSummary(BigDecimal monthlyGross, BigDecimal monthlyTax, BigDecimal monthlyNet,
                               BigDecimal monthlyExpenses, BigDecimal monthlyEmis, BigDecimal monthlySavingsTarget,
                               BigDecimal disposableIncome, BigDecimal freeCashAfterSavings,
                               BigDecimal requiredGross, BigDecimal grossGap,
                               BigDecimal emiToIncomePct, BigDecimal savingsRatePct,
                               Affordability affordability, HealthScore health) {}
