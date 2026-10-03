package com.finplan.finance.service;

import com.finplan.finance.domain.Income;
import com.finplan.finance.domain.Repositories.IncomeRepository;
import com.finplan.finance.web.ApiException;
import com.finplan.finance.web.Dtos.IncomeRequest;
import com.finplan.finance.web.Dtos.IncomeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IncomeService {
    private final IncomeRepository repo;
    private final ChangeRecorder changes;

    public IncomeService(IncomeRepository repo, ChangeRecorder changes) {
        this.repo = repo;
        this.changes = changes;
    }

    @Transactional(readOnly = true)
    public List<Income> entities(UUID userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }

    @Transactional(readOnly = true)
    public List<IncomeResponse> list(UUID userId) { return entities(userId).stream().map(this::toResponse).toList(); }

    @Transactional
    public IncomeResponse create(UUID userId, IncomeRequest r) {
        Income i = new Income();
        i.setUserId(userId);
        apply(i, r);
        repo.save(i);
        changes.record(userId, "INCOME", i.getId(), "CREATED", Map.of("type", i.getType().name()));
        return toResponse(i);
    }

    @Transactional
    public IncomeResponse update(UUID userId, UUID id, IncomeRequest r) {
        Income i = find(userId, id);
        apply(i, r);
        repo.save(i);
        changes.record(userId, "INCOME", id, "UPDATED", Map.of("type", i.getType().name()));
        return toResponse(i);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repo.delete(find(userId, id));
        changes.record(userId, "INCOME", id, "DELETED", Map.of());
    }

    private Income find(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Income not found"));
    }

    private void apply(Income i, IncomeRequest r) {
        i.setType(r.type());
        i.setLabel(r.label().trim());
        i.setAmount(r.amount());
        i.setFrequency(r.frequency());
    }

    private IncomeResponse toResponse(Income i) {
        return new IncomeResponse(i.getId().toString(), i.getType(), i.getLabel(), i.getAmount(), i.getFrequency(), i.monthlyAmount());
    }
}
