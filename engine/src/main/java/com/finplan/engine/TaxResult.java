package com.finplan.engine;

import java.math.BigDecimal;

public record TaxResult(BigDecimal annualGross, BigDecimal taxableIncome, BigDecimal incomeTax,
                        BigDecimal cess, BigDecimal totalTax, BigDecimal effectiveRatePct) {}
