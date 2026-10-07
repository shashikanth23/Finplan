package com.finplan.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consumes finance events. Delivery from Kafka is at-least-once, so event IDs are persisted before handling.
 * Malformed messages throw, and the error handler retries then parks them on finance-events.DLT.
 * "Sending" is a log line: plug an email/SMS/push provider in at deliver().
 */
@Component
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final ObjectMapper mapper;
    private final ProcessedEventStore processedEvents;

    public NotificationListener(ObjectMapper mapper, ProcessedEventStore processedEvents) {
        this.mapper = mapper;
        this.processedEvents = processedEvents;
    }

    @KafkaListener(topics = "finance-events")
    @Transactional
    public void onEvent(String message) throws Exception {
        JsonNode event = mapper.readTree(message);
        String eventId = event.path("eventId").asText(null);
        String userId = event.path("userId").asText(null);
        if (eventId == null || userId == null) throw new IllegalArgumentException("Event missing eventId/userId");
        UUID parsedEventId = UUID.fromString(eventId);

        if (!processedEvents.markIfNew(parsedEventId)) {
            log.debug("Duplicate event {} ignored", eventId);
            return;
        }
        NotificationFormatter.format(event).ifPresent(text -> deliver(userId, text));
    }

    void deliver(String userId, String text) {
        log.info("NOTIFY user={} message=\"{}\"", userId, text);
    }
}
