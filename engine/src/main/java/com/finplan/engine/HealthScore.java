package com.finplan.engine;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 0-100 score: EMI burden (35), savings rate (35), expense ratio (30). Rule-based so every point is explainable. */
public record HealthScore(int score, String grade, List<String> recommendations) {

    private static final NumberFormat INR = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN"));

    public static HealthScore compute(BigDecimal net, BigDecimal expenses, BigDecimal emis, BigDecimal savings) {
        if (net.signum() <= 0) {
            return new HealthScore(0, "Critical", List.of("Add your income to get a financial health score."));
        }
        double n = net.doubleValue();
        double emiRatio = emis.doubleValue() / n;
        double expenseRatio = expenses.doubleValue() / n;
        double savingsRate = savings.doubleValue() / n;

        double emiPts = 35 * scale(emiRatio, 0.60, 0.30);          // 30% or less: full marks, 60%+: zero
        double savingsPts = 35 * scale(savingsRate, 0.0, 0.20);    // 20%+ saved: full marks
        double expensePts = 30 * scale(expenseRatio, 0.90, 0.50);  // 50% or less: full marks, 90%+: zero
        int score = (int) Math.round(emiPts + savingsPts + expensePts);

        BigDecimal left = net.subtract(expenses).subtract(emis).subtract(savings);
        List<String> tips = new ArrayList<>();
        if (left.signum() < 0) {
            score = Math.min(score, 40);
            tips.add("You are short by Rs " + INR.format(left.negate().setScale(0, java.math.RoundingMode.CEILING))
                    + " a month. Reduce expenses or lower the savings target until this is positive.");
        }
        if (emiRatio > 0.40) {
            tips.add("EMIs take " + Math.round(emiRatio * 100) + "% of take-home, above the 40% lenders consider safe. "
                    + "Avoid new loans and consider prepaying the highest-rate one.");
        } else if (emiRatio > 0.30) {
            tips.add("EMIs are " + Math.round(emiRatio * 100) + "% of take-home. Keep new borrowing low.");
        }
        if (savingsRate < 0.10) {
            tips.add("You save " + Math.round(savingsRate * 100) + "% of take-home. Aim for at least 20%.");
        }
        if (expenseRatio > 0.60) {
            tips.add("Living expenses use " + Math.round(expenseRatio * 100) + "% of take-home. Review lifestyle and variable costs first.");
        }
        if (tips.isEmpty()) tips.add("Your finances look balanced. Keep the savings rate steady as income grows.");

        String grade = score >= 80 ? "Excellent" : score >= 60 ? "Good" : score >= 40 ? "Needs attention" : "Critical";
        return new HealthScore(score, grade, List.copyOf(tips));
    }

    /** 1.0 at {@code good}, 0.0 at {@code bad}, linear in between, clamped. Works for either direction. */
    private static double scale(double value, double bad, double good) {
        double t = (value - bad) / (good - bad);
        return Math.max(0, Math.min(1, t));
    }
}
