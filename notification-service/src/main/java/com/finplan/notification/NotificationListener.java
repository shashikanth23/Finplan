package com.finplan.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Consumes finance events. Delivery from Kafka is at-least-once, so events are deduplicated by eventId.
 * Malformed messages throw, and the error handler retries then parks them on finance-events.DLT.
 * "Sending" is a log line: plug an email/SMS/push provider in at deliver().
 */
@Component
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private static final int REMEMBER_LAST = 10_000;

    private final ObjectMapper mapper;
    // In-memory and per-instance. With several replicas, move this to Redis or a table keyed by eventId.
    private final Set<String> seen = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>() {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > REMEMBER_LAST;
                }
            }));

    public NotificationListener(ObjectMapper mapper) { this.mapper = mapper; }

    @KafkaListener(topics = "finance-events")
    public void onEvent(String message) throws Exception {
        JsonNode event = mapper.readTree(message);
        String eventId = event.path("eventId").asText(null);
        String userId = event.path("userId").asText(null);
        if (eventId == null || userId == null) throw new IllegalArgumentException("Event missing eventId/userId");

        if (!seen.add(eventId)) {
            log.debug("Duplicate event {} ignored", eventId);
            return;
        }
        NotificationFormatter.format(event).ifPresent(text -> deliver(userId, text));
    }

    void deliver(String userId, String text) {
        log.info("NOTIFY user={} message=\"{}\"", userId, text);
    }
}
