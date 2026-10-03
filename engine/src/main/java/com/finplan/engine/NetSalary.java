package com.finplan.engine;

import java.math.BigDecimal;

public record NetSalary(BigDecimal monthlyGross, BigDecimal monthlyTax,
                        BigDecimal monthlyFixedDeductions, BigDecimal monthlyNet) {}
