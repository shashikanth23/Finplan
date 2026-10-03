package com.finplan.finance.domain;

import com.finplan.engine.TaxRegime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter @Setter @NoArgsConstructor
public class Profile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 3)
    private TaxRegime taxRegime = TaxRegime.NEW;

    @Column(name = "monthly_fixed_deductions", nullable = false)
    private BigDecimal monthlyFixedDeductions = BigDecimal.ZERO;

    @Column(name = "annual_old_regime_deductions", nullable = false)
    private BigDecimal annualOldRegimeDeductions = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Profile(UUID userId) { this.userId = userId; }
}
