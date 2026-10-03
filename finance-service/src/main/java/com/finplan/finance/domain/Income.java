package com.finplan.finance.domain;

import com.finplan.finance.domain.Enums.Frequency;
import com.finplan.finance.domain.Enums.IncomeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incomes")
@Getter @Setter @NoArgsConstructor
public class Income {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncomeType type;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Frequency frequency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Bonuses and other annual payments are spread over 12 months for budgeting. */
    public BigDecimal monthlyAmount() {
        return frequency == Frequency.ANNUAL ? amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP) : amount;
    }
}
