package com.finplan.engine;

import java.math.BigDecimal;

public record ScheduleRow(int month, BigDecimal payment, BigDecimal interest, BigDecimal principal, BigDecimal balance) {}
