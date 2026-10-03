package com.finplan.finance.domain;

import com.finplan.finance.domain.Enums.LoanType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "debts")
@Getter @Setter @NoArgsConstructor
public class Debt {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    @Column(length = 100)
    private String lender;

    @Column(nullable = false)
    private BigDecimal principal;

    @Column(name = "annual_rate", nullable = false)
    private BigDecimal annualRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false)
    private BigDecimal emi;

    @Column(name = "remaining_balance", nullable = false)
    private BigDecimal remainingBalance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
