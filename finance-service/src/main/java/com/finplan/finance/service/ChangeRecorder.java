package com.finplan.finance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finplan.finance.domain.OutboxEvent;
import com.finplan.finance.domain.Repositories.OutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Called from inside a service transaction whenever user data changes. It
 * (1) writes an outbox row in the SAME transaction, so the event exists if and only if the change committed, and
 * (2) evicts the cached dashboard only after commit, so a rolled-back change never clears a valid cache.
 */
@Component
public class ChangeRecorder {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final DashboardCache cache;

    public ChangeRecorder(OutboxRepository outbox, ObjectMapper mapper, DashboardCache cache) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.cache = cache;
    }

    public void record(UUID userId, String aggregate, UUID aggregateId, String action, Map<String, Object> data) {
        FinanceEvent event = new FinanceEvent(UUID.randomUUID(), aggregate + "_CHANGED", action, userId,
                aggregateId, Instant.now(), data);
        OutboxEvent row = new OutboxEvent();
        row.setId(event.eventId());
        row.setAggregateType(aggregate);
        row.setAggregateId(aggregateId);
        row.setEventType(event.type());
        row.setUserId(userId);
        try {
            row.setPayload(mapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise event", e);
        }
        outbox.save(row);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { cache.evict(userId); }
            });
        } else {
            cache.evict(userId);
        }
    }
}
