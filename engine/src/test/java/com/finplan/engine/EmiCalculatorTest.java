package com.finplan.engine;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmiCalculatorTest {
    @Test void tenLakhAt8Point5PercentFor20Years() {
        BigDecimal emi = EmiCalculator.emi(new BigDecimal("1000000"), new BigDecimal("8.5"), 240);
        assertEquals(0, new BigDecimal("8678.23").compareTo(emi));
    }

    @Test void zeroInterestSplitsEvenly() {
        assertEquals(0, new BigDecimal("1000.00").compareTo(EmiCalculator.emi(new BigDecimal("12000"), BigDecimal.ZERO, 12)));
    }

    @Test void affordablePrincipalInvertsEmi() {
        BigDecimal p = EmiCalculator.affordablePrincipal(new BigDecimal("8678.23"), new BigDecimal("8.5"), 240);
        assertTrue(p.subtract(new BigDecimal("1000000")).abs().compareTo(new BigDecimal("5")) < 0);
    }

    @Test void scheduleEndsAtZeroAndInterestAddsUp() {
        List<ScheduleRow> rows = EmiCalculator.schedule(new BigDecimal("500000"), new BigDecimal("9"), 60);
        assertEquals(60, rows.size());
        assertEquals(0, rows.get(59).balance().signum());
        BigDecimal principalSum = rows.stream().map(ScheduleRow::principal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("500000.00").compareTo(principalSum));
    }

    @Test void remainingBalanceAgreesWithSchedule() {
        List<ScheduleRow> rows = EmiCalculator.schedule(new BigDecimal("500000"), new BigDecimal("9"), 60);
        BigDecimal closed = EmiCalculator.remainingBalance(new BigDecimal("500000"), new BigDecimal("9"), 60, 24);
        assertTrue(closed.subtract(rows.get(23).balance()).abs().compareTo(new BigDecimal("2")) < 0);
    }

    @Test void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> EmiCalculator.emi(BigDecimal.ZERO, BigDecimal.TEN, 12));
        assertThrows(IllegalArgumentException.class, () -> EmiCalculator.emi(BigDecimal.TEN, BigDecimal.TEN, 0));
    }
}
