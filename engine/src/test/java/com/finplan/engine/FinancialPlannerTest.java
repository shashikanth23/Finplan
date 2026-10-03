package com.finplan.engine;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class FinancialPlannerTest {
    private final FinancialPlanner planner = new FinancialPlanner();
    private static BigDecimal n(String v) { return new BigDecimal(v); }

    private FinancialInput input(String gross, String expenses, String emis, String savings) {
        return new FinancialInput(n(gross), new SalaryParams(TaxRegime.NEW, n("10000"), null), n(expenses), n(emis), n(savings));
    }

    @Test void summaryForProductExample() {
        FinancialSummary s = planner.summarize(input("80000", "35000", "20000", "15000"));
        assertEquals(0, n("70000.00").compareTo(s.monthlyNet()));
        assertEquals(0, n("0.00").compareTo(s.freeCashAfterSavings()));
        assertEquals(0, n("80000").compareTo(s.requiredGross()));
        assertEquals(0, s.grossGap().signum());
    }

    @Test void shortfallShowsUpAsRaiseNeeded() {
        FinancialSummary s = planner.summarize(input("60000", "35000", "20000", "15000"));
        assertTrue(s.freeCashAfterSavings().signum() < 0);
        assertTrue(s.grossGap().signum() > 0);
        assertTrue(s.health().score() <= 40);
    }

    @Test void healthyProfileScoresWell() {
        FinancialSummary s = planner.summarize(input("150000", "45000", "10000", "40000"));
        assertTrue(s.health().score() >= 80, "score was " + s.health().score());
    }

    @Test void scenarioExtraEmiLowersScoreAndFreeCash() {
        FinancialInput base = input("120000", "40000", "10000", "20000");
        ScenarioResult r = planner.simulate(base, new Scenario(null, n("35000"), null, null));
        assertTrue(r.freeCashDelta().compareTo(n("-35000")) == 0);
        assertTrue(r.scoreDelta() < 0);
    }

    @Test void scenarioRaiseIncreasesTakeHome() {
        ScenarioResult r = planner.simulate(input("100000", "40000", "10000", "20000"), new Scenario(n("20"), null, null, null));
        assertTrue(r.netDelta().signum() > 0);
    }

    @Test void negativeInputRejected() {
        assertThrows(IllegalArgumentException.class, () -> input("-1", "0", "0", "0"));
    }
}
