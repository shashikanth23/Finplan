package com.finplan.planner.web;

import com.finplan.engine.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Thin HTTP layer: all the maths lives in the engine module, which has its own tests. */
@RestController
@RequestMapping("/api/planner")
public class PlannerController {

    private final TaxCalculator taxCalc = new TaxCalculator();
    private final SalaryCalculator salaryCalc = new SalaryCalculator();
    private final FinancialPlanner planner = new FinancialPlanner();

    public record TaxRequest(BigDecimal annualGross, TaxRegime regime, BigDecimal oldRegimeDeductions) {}

    @PostMapping("/tax")
    public TaxResult tax(@RequestBody TaxRequest r) {
        return taxCalc.annualTax(need(r.annualGross(), "annualGross"), r.regime(), r.oldRegimeDeductions());
    }

    public record NetRequest(BigDecimal monthlyGross, SalaryParams salary) {}

    @PostMapping("/salary/net")
    public NetSalary net(@RequestBody NetRequest r) {
        return salaryCalc.net(need(r.monthlyGross(), "monthlyGross"), params(r.salary()));
    }

    public record RequiredSalaryRequest(BigDecimal targetNetMonthly, SalaryParams salary) {}
    public record RequiredSalaryResponse(BigDecimal requiredMonthlyGross, NetSalary breakdown) {}

    /** "What salary do I need?" */
    @PostMapping("/salary/required")
    public RequiredSalaryResponse required(@RequestBody RequiredSalaryRequest r) {
        SalaryParams p = params(r.salary());
        BigDecimal gross = salaryCalc.requiredGross(need(r.targetNetMonthly(), "targetNetMonthly"), p);
        return new RequiredSalaryResponse(gross, salaryCalc.net(gross, p));
    }

    public record EmiRequest(BigDecimal principal, BigDecimal annualRatePct, Integer months, Boolean includeSchedule) {}
    public record EmiResponse(BigDecimal emi, BigDecimal totalPayable, BigDecimal totalInterest, List<ScheduleRow> schedule) {}

    @PostMapping("/emi")
    public EmiResponse emi(@RequestBody EmiRequest r) {
        BigDecimal principal = need(r.principal(), "principal");
        BigDecimal rate = need(r.annualRatePct(), "annualRatePct");
        int months = need(r.months(), "months");
        BigDecimal emi = EmiCalculator.emi(principal, rate, months);
        List<ScheduleRow> rows = EmiCalculator.schedule(principal, rate, months);
        BigDecimal payable = rows.stream().map(ScheduleRow::payment).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interest = rows.stream().map(ScheduleRow::interest).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EmiResponse(emi, payable, interest, Boolean.TRUE.equals(r.includeSchedule()) ? rows : null);
    }

    public record LoanSizeRequest(BigDecimal emi, BigDecimal annualRatePct, Integer months) {}
    public record LoanSizeResponse(BigDecimal principal) {}

    /** "What EMI can I afford" turned into "what loan does that buy". */
    @PostMapping("/emi/affordable-loan")
    public LoanSizeResponse loanSize(@RequestBody LoanSizeRequest r) {
        return new LoanSizeResponse(EmiCalculator.affordablePrincipal(
                need(r.emi(), "emi"), need(r.annualRatePct(), "annualRatePct"), need(r.months(), "months")));
    }

    public record AffordabilityRequest(BigDecimal netMonthly, BigDecimal expenses, BigDecimal existingEmis, BigDecimal savings) {}

    @PostMapping("/affordability")
    public Affordability affordability(@RequestBody AffordabilityRequest r) {
        return Affordability.compute(need(r.netMonthly(), "netMonthly"), Money.nz(r.expenses()),
                Money.nz(r.existingEmis()), Money.nz(r.savings()));
    }

    @PostMapping("/summary")
    public FinancialSummary summary(@RequestBody FinancialInput in) {
        return planner.summarize(in);
    }

    public record ScenarioRequest(FinancialInput base, Scenario scenario) {}

    /** "What happens if my salary changes?" */
    @PostMapping("/scenario")
    public ScenarioResult scenario(@RequestBody ScenarioRequest r) {
        return planner.simulate(need(r.base(), "base"), need(r.scenario(), "scenario"));
    }

    public record CategorizeRequest(List<String> descriptions) {}
    public record Categorized(String description, String category) {}

    @PostMapping("/categorize")
    public List<Categorized> categorize(@RequestBody CategorizeRequest r) {
        List<String> in = need(r.descriptions(), "descriptions");
        if (in.size() > 500) throw new IllegalArgumentException("At most 500 descriptions per request");
        return in.stream().map(d -> new Categorized(d, ExpenseCategorizer.categorize(d))).toList();
    }

    private static SalaryParams params(SalaryParams p) { return p == null ? SalaryParams.defaults() : p; }

    private static <T> T need(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
