package com.finplan.finance;

import com.finplan.finance.domain.GoalMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalMathTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Test void spreadsRemainingAmountOverMonthsLeft() {
        // 120,000 target, 20,000 saved, due Oct 2027 = 12 months -> 100,000 / 12 = 8,333.34 (rounded up)
        BigDecimal r = GoalMath.requiredMonthly(new BigDecimal("120000"), new BigDecimal("20000"), TODAY, LocalDate.of(2027, 10, 1));
        assertEquals(0, new BigDecimal("8333.34").compareTo(r));
    }

    @Test void reachedGoalNeedsNothing() {
        assertEquals(0, GoalMath.requiredMonthly(new BigDecimal("1000"), new BigDecimal("1000"), TODAY, LocalDate.of(2027, 1, 1)).signum());
    }

    @Test void overdueGoalNeedsNothing() {
        assertEquals(0, GoalMath.requiredMonthly(new BigDecimal("1000"), BigDecimal.ZERO, TODAY, LocalDate.of(2026, 1, 1)).signum());
    }

    @Test void goalDueThisMonthStillCountsOneMonth() {
        assertEquals(1, GoalMath.monthsLeft(TODAY, LocalDate.of(2026, 10, 25)));
    }

    @Test void progressIsCappedAtHundred() {
        assertEquals(0, new BigDecimal("100").compareTo(GoalMath.progressPct(new BigDecimal("100"), new BigDecimal("250"))));
    }
}
