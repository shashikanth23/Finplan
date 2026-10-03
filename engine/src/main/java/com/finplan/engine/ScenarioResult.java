package com.finplan.engine;

import java.math.BigDecimal;

public record ScenarioResult(FinancialSummary base, FinancialSummary scenario,
                             BigDecimal netDelta, BigDecimal freeCashDelta, int scoreDelta) {}
