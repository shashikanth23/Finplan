package com.finplan.finance.service;

import com.finplan.engine.FinancialInput;
import com.finplan.engine.FinancialSummary;
import com.finplan.engine.SalaryParams;
import com.finplan.finance.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    public record DashboardResponse(FinancialSummary summary, Map<String, BigDecimal> expensesByCategory,
                                    Map<String, BigDecimal> incomeByType, BigDecimal totalDebtRemaining,
                                    int activeGoals, String source) {}

    private final ProfileService profiles;
    private final IncomeService incomes;
    private final ExpenseService expenses;
    private final DebtService debts;
    private final GoalService goals;
    private final PlannerClient planner;
    private final DashboardCache cache;

    public DashboardService(ProfileService profiles, IncomeService incomes, ExpenseService expenses, DebtService debts,
                            GoalService goals, PlannerClient planner, DashboardCache cache) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
        this.debts = debts;
        this.goals = goals;
        this.planner = planner;
        this.cache = cache;
    }

    public DashboardResponse get(UUID userId, String bearerToken) {
        return cache.get(userId).orElseGet(() -> {
            DashboardResponse fresh = compute(userId, bearerToken);
            // Don't cache a degraded answer for the full TTL: the next request should retry the planner.
            if ("planner-service".equals(fresh.source())) cache.put(userId, fresh);
            return fresh;
        });
    }

    DashboardResponse compute(UUID userId, String bearerToken) {
        Profile profile = profiles.get(userId);
        List<Income> inc = incomes.entities(userId);
        List<Expense> exp = expenses.entities(userId);
        List<Debt> dbt = debts.entities(userId);
        List<Goal> gls = goals.entities(userId);

        BigDecimal gross = inc.stream().map(Income::monthlyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenseTotal = exp.stream().map(Expense::getMonthlyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal emiTotal = dbt.stream().map(Debt::getEmi).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate today = LocalDate.now();
        BigDecimal savingsTarget = gls.stream()
                .map(g -> GoalMath.requiredMonthly(g.getTargetAmount(), g.getSavedAmount(), today, g.getTargetDate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinancialInput input = new FinancialInput(gross,
                new SalaryParams(profile.getTaxRegime(), profile.getMonthlyFixedDeductions(), profile.getAnnualOldRegimeDeductions()),
                expenseTotal, emiTotal, savingsTarget);
        PlannerClient.Result result = planner.summarize(input, bearerToken);

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        exp.forEach(e -> byCategory.merge(e.getCategory().name(), e.getMonthlyAmount(), BigDecimal::add));
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        inc.forEach(i -> byType.merge(i.getType().name(), i.monthlyAmount(), BigDecimal::add));
        BigDecimal debtRemaining = dbt.stream().map(Debt::getRemainingBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        int active = (int) gls.stream().filter(g -> g.getSavedAmount().compareTo(g.getTargetAmount()) < 0).count();

        return new DashboardResponse(result.summary(), byCategory, byType, debtRemaining, active, result.source());
    }
}
