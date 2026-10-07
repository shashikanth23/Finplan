package com.finplan.finance.service;

import com.finplan.finance.domain.OutboxEvent;
import com.finplan.finance.domain.Repositories.OutboxRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays outbox rows to Kafka in creation order. A row is marked published only after the broker acks it,
 * so a crash between send and mark causes a duplicate, never a loss (at-least-once; consumers dedupe by eventId).
 * Rows are locked with SELECT ... FOR UPDATE SKIP LOCKED in the publishing transaction, and later events for a
 * user remain blocked until that user's earlier events are published.
 */
@Component
@ConditionalOnProperty(name = "finplan.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    public static final String TOPIC = "finance-events";
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${finplan.outbox.poll-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outbox.lockNextBatch();
        for (OutboxEvent e : batch) {
            try {
                // Key by user so one user's events stay ordered within a partition.
                kafka.send(TOPIC, e.getUserId().toString(), e.getPayload()).get(5, TimeUnit.SECONDS);
                outbox.markPublished(e.getId(), Instant.now());
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                log.warn("Outbox publish failed at event {}, will retry next poll: {}", e.getId(), ex.toString());
                return; // stop here to preserve ordering
            }
        }
    }

    @Configuration
    static class TopicConfig {
        @Bean
        NewTopic financeEventsTopic() {
            return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
        }
    }
}
