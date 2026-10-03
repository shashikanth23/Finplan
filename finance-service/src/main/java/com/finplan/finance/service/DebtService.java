package com.finplan.finance.service;

import com.finplan.engine.EmiCalculator;
import com.finplan.finance.domain.Debt;
import com.finplan.finance.domain.Repositories.DebtRepository;
import com.finplan.finance.web.ApiException;
import com.finplan.finance.web.Dtos.DebtRequest;
import com.finplan.finance.web.Dtos.DebtResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DebtService {
    private final DebtRepository repo;
    private final ChangeRecorder changes;

    public DebtService(DebtRepository repo, ChangeRecorder changes) {
        this.repo = repo;
        this.changes = changes;
    }

    @Transactional(readOnly = true)
    public List<Debt> entities(UUID userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }

    @Transactional(readOnly = true)
    public List<DebtResponse> list(UUID userId) { return entities(userId).stream().map(this::toResponse).toList(); }

    @Transactional
    public DebtResponse create(UUID userId, DebtRequest r) {
        Debt d = new Debt();
        d.setUserId(userId);
        apply(d, r);
        repo.save(d);
        changes.record(userId, "DEBT", d.getId(), "CREATED", Map.of("loanType", d.getLoanType().name()));
        return toResponse(d);
    }

    @Transactional
    public DebtResponse update(UUID userId, UUID id, DebtRequest r) {
        Debt d = find(userId, id);
        apply(d, r);
        repo.save(d);
        changes.record(userId, "DEBT", id, "UPDATED", Map.of("loanType", d.getLoanType().name()));
        return toResponse(d);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repo.delete(find(userId, id));
        changes.record(userId, "DEBT", id, "DELETED", Map.of());
    }

    private Debt find(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan not found"));
    }

    private void apply(Debt d, DebtRequest r) {
        d.setLoanType(r.loanType());
        d.setLender(r.lender() == null ? null : r.lender().trim());
        d.setPrincipal(r.principal());
        d.setAnnualRate(r.annualRate());
        d.setTenureMonths(r.tenureMonths());
        d.setEmi(r.emi() != null ? r.emi() : EmiCalculator.emi(r.principal(), r.annualRate(), r.tenureMonths()));
        d.setRemainingBalance(r.remainingBalance() != null ? r.remainingBalance() : r.principal());
    }

    private DebtResponse toResponse(Debt d) {
        return new DebtResponse(d.getId().toString(), d.getLoanType(), d.getLender(), d.getPrincipal(), d.getAnnualRate(),
                d.getTenureMonths(), d.getEmi(), d.getRemainingBalance());
    }
}
