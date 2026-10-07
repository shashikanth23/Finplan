package com.finplan.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcProcessedEventStore implements ProcessedEventStore {
    private final JdbcTemplate jdbc;

    public JdbcProcessedEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markIfNew(UUID eventId) {
        return jdbc.update("insert into processed_events (event_id) values (?) on conflict do nothing", eventId) == 1;
    }
}
