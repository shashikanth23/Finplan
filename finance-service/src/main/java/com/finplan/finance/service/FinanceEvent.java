package com.finplan.finance.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Wire format published to Kafka. Consumers dedupe on eventId (delivery is at-least-once). */
public record FinanceEvent(UUID eventId, String type, String action, UUID userId, UUID aggregateId,
                           Instant occurredAt, Map<String, Object> data) {}
