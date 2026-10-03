package com.finplan.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pure helpers for savings goals, kept free of Spring/JPA so they are trivially testable. */
public final class GoalMath {
    private GoalMath() {}

    /** Whole months left until the target date, at least 1 (a goal due this month still needs a contribution). */
    public static long monthsLeft(LocalDate today, LocalDate targetDate) {
        return Math.max(1, ChronoUnit.MONTHS.between(today.withDayOfMonth(1), targetDate.withDayOfMonth(1)));
    }

    /** Monthly amount to set aside to hit the target on time; zero once the goal is reached or overdue. */
    public static BigDecimal requiredMonthly(BigDecimal target, BigDecimal saved, LocalDate today, LocalDate targetDate) {
        BigDecimal remaining = target.subtract(saved);
        if (remaining.signum() <= 0 || targetDate.isBefore(today)) return BigDecimal.ZERO.setScale(2);
        return remaining.divide(BigDecimal.valueOf(monthsLeft(today, targetDate)), 2, RoundingMode.CEILING);
    }

    public static BigDecimal progressPct(BigDecimal target, BigDecimal saved) {
        if (target.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        return saved.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100));
    }
}
