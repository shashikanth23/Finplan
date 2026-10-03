package com.finplan.finance.web;

import com.finplan.engine.TaxRegime;
import com.finplan.finance.domain.Enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Dtos {
    private Dtos() {}

    public record ProfileRequest(
            @NotNull TaxRegime taxRegime,
            @NotNull @DecimalMin("0") BigDecimal monthlyFixedDeductions,
            @NotNull @DecimalMin("0") BigDecimal annualOldRegimeDeductions) {}
    public record ProfileResponse(TaxRegime taxRegime, BigDecimal monthlyFixedDeductions, BigDecimal annualOldRegimeDeductions) {}

    public record IncomeRequest(
            @NotNull IncomeType type,
            @NotBlank @Size(max = 100) String label,
            @NotNull @DecimalMin("0") BigDecimal amount,
            @NotNull Frequency frequency) {}
    public record IncomeResponse(String id, IncomeType type, String label, BigDecimal amount,
                                 Frequency frequency, BigDecimal monthlyAmount) {}

    public record ExpenseRequest(
            @NotNull ExpenseCategory category,
            @NotNull ExpenseKind kind,
            @NotBlank @Size(max = 100) String label,
            @NotNull @DecimalMin("0") BigDecimal monthlyAmount) {}
    public record ExpenseResponse(String id, ExpenseCategory category, ExpenseKind kind, String label, BigDecimal monthlyAmount) {}

    /** emi and remainingBalance are optional: the EMI is computed from the loan terms, the balance defaults to principal. */
    public record DebtRequest(
            @NotNull LoanType loanType,
            @Size(max = 100) String lender,
            @NotNull @DecimalMin("1") BigDecimal principal,
            @NotNull @DecimalMin("0") @DecimalMax("60") BigDecimal annualRate,
            @NotNull @Min(1) @Max(600) Integer tenureMonths,
            @DecimalMin("0") BigDecimal emi,
            @DecimalMin("0") BigDecimal remainingBalance) {}
    public record DebtResponse(String id, LoanType loanType, String lender, BigDecimal principal, BigDecimal annualRate,
                               Integer tenureMonths, BigDecimal emi, BigDecimal remainingBalance) {}

    public record GoalRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin("1") BigDecimal targetAmount,
            @NotNull @DecimalMin("0") BigDecimal savedAmount,
            @NotNull LocalDate targetDate) {}
    public record GoalResponse(String id, String name, BigDecimal targetAmount, BigDecimal savedAmount, LocalDate targetDate,
                               BigDecimal progressPct, BigDecimal requiredMonthly, boolean reached) {}
}
