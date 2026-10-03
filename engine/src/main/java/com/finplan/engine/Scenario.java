package com.finplan.engine;

import java.math.BigDecimal;

/** "What if...": every field is optional (null means no change). */
public record Scenario(BigDecimal grossChangePct, BigDecimal extraMonthlyEmi,
                       BigDecimal expenseChangePct, BigDecimal newSavingsTarget) {}
