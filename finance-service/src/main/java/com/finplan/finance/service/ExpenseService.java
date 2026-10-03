package com.finplan.finance.service;

import com.finplan.finance.domain.Expense;
import com.finplan.finance.domain.Repositories.ExpenseRepository;
import com.finplan.finance.web.ApiException;
import com.finplan.finance.web.Dtos.ExpenseRequest;
import com.finplan.finance.web.Dtos.ExpenseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseService {
    private final ExpenseRepository repo;
    private final ChangeRecorder changes;

    public ExpenseService(ExpenseRepository repo, ChangeRecorder changes) {
        this.repo = repo;
        this.changes = changes;
    }

    @Transactional(readOnly = true)
    public List<Expense> entities(UUID userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID userId) { return entities(userId).stream().map(this::toResponse).toList(); }

    @Transactional
    public ExpenseResponse create(UUID userId, ExpenseRequest r) {
        Expense e = new Expense();
        e.setUserId(userId);
        apply(e, r);
        repo.save(e);
        changes.record(userId, "EXPENSE", e.getId(), "CREATED", Map.of("category", e.getCategory().name()));
        return toResponse(e);
    }

    @Transactional
    public ExpenseResponse update(UUID userId, UUID id, ExpenseRequest r) {
        Expense e = find(userId, id);
        apply(e, r);
        repo.save(e);
        changes.record(userId, "EXPENSE", id, "UPDATED", Map.of("category", e.getCategory().name()));
        return toResponse(e);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repo.delete(find(userId, id));
        changes.record(userId, "EXPENSE", id, "DELETED", Map.of());
    }

    private Expense find(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private void apply(Expense e, ExpenseRequest r) {
        e.setCategory(r.category());
        e.setKind(r.kind());
        e.setLabel(r.label().trim());
        e.setMonthlyAmount(r.monthlyAmount());
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(e.getId().toString(), e.getCategory(), e.getKind(), e.getLabel(), e.getMonthlyAmount());
    }
}
