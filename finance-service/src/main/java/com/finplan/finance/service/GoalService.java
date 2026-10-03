package com.finplan.finance.service;

import com.finplan.finance.domain.Goal;
import com.finplan.finance.domain.GoalMath;
import com.finplan.finance.domain.Repositories.GoalRepository;
import com.finplan.finance.web.ApiException;
import com.finplan.finance.web.Dtos.GoalRequest;
import com.finplan.finance.web.Dtos.GoalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoalService {
    private final GoalRepository repo;
    private final ChangeRecorder changes;

    public GoalService(GoalRepository repo, ChangeRecorder changes) {
        this.repo = repo;
        this.changes = changes;
    }

    @Transactional(readOnly = true)
    public List<Goal> entities(UUID userId) { return repo.findByUserIdOrderByTargetDateAsc(userId); }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID userId) { return entities(userId).stream().map(this::toResponse).toList(); }

    @Transactional
    public GoalResponse create(UUID userId, GoalRequest r) {
        Goal g = new Goal();
        g.setUserId(userId);
        apply(g, r);
        repo.save(g);
        changes.record(userId, "GOAL", g.getId(), "CREATED", data(g));
        return toResponse(g);
    }

    @Transactional
    public GoalResponse update(UUID userId, UUID id, GoalRequest r) {
        Goal g = find(userId, id);
        apply(g, r);
        repo.save(g);
        changes.record(userId, "GOAL", id, "UPDATED", data(g));
        return toResponse(g);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repo.delete(find(userId, id));
        changes.record(userId, "GOAL", id, "DELETED", Map.of());
    }

    private Goal find(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found"));
    }

    private void apply(Goal g, GoalRequest r) {
        g.setName(r.name().trim());
        g.setTargetAmount(r.targetAmount());
        g.setSavedAmount(r.savedAmount());
        g.setTargetDate(r.targetDate());
    }

    private Map<String, Object> data(Goal g) {
        return Map.of("goalName", g.getName(), "goalReached", g.getSavedAmount().compareTo(g.getTargetAmount()) >= 0);
    }

    private GoalResponse toResponse(Goal g) {
        LocalDate today = LocalDate.now();
        return new GoalResponse(g.getId().toString(), g.getName(), g.getTargetAmount(), g.getSavedAmount(), g.getTargetDate(),
                GoalMath.progressPct(g.getTargetAmount(), g.getSavedAmount()),
                GoalMath.requiredMonthly(g.getTargetAmount(), g.getSavedAmount(), today, g.getTargetDate()),
                g.getSavedAmount().compareTo(g.getTargetAmount()) >= 0);
    }
}
