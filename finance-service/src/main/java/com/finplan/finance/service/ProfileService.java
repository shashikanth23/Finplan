package com.finplan.finance.service;

import com.finplan.finance.domain.Profile;
import com.finplan.finance.domain.Repositories.ProfileRepository;
import com.finplan.finance.web.Dtos.ProfileRequest;
import com.finplan.finance.web.Dtos.ProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileService {
    private final ProfileRepository repo;
    private final ChangeRecorder changes;

    public ProfileService(ProfileRepository repo, ChangeRecorder changes) {
        this.repo = repo;
        this.changes = changes;
    }

    /** A user with no saved profile gets the defaults (new regime, no deductions) without a row being created. */
    @Transactional(readOnly = true)
    public Profile get(UUID userId) {
        return repo.findById(userId).orElseGet(() -> new Profile(userId));
    }

    @Transactional(readOnly = true)
    public ProfileResponse view(UUID userId) { return toResponse(get(userId)); }

    @Transactional
    public ProfileResponse save(UUID userId, ProfileRequest r) {
        Profile p = repo.findById(userId).orElseGet(() -> new Profile(userId));
        p.setTaxRegime(r.taxRegime());
        p.setMonthlyFixedDeductions(r.monthlyFixedDeductions());
        p.setAnnualOldRegimeDeductions(r.annualOldRegimeDeductions());
        p.setUpdatedAt(Instant.now());
        repo.save(p);
        changes.record(userId, "PROFILE", userId, "UPDATED", Map.of("taxRegime", p.getTaxRegime().name()));
        return toResponse(p);
    }

    private ProfileResponse toResponse(Profile p) {
        return new ProfileResponse(p.getTaxRegime(), p.getMonthlyFixedDeductions(), p.getAnnualOldRegimeDeductions());
    }
}
