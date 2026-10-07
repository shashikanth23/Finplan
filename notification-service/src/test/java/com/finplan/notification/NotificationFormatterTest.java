package com.finplan.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationFormatterTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private String fmt(String json) throws Exception {
        return NotificationFormatter.format(mapper.readTree(json)).orElse(null);
    }

    @Test void goalReachedProducesMessage() throws Exception {
        String m = fmt("{\"type\":\"GOAL_CHANGED\",\"action\":\"UPDATED\",\"data\":{\"goalName\":\"Laptop\",\"goalReached\":true}}");
        assertTrue(m.contains("Laptop"));
    }

    @Test void goalInProgressIsSilent() throws Exception {
        assertNull(fmt("{\"type\":\"GOAL_CHANGED\",\"action\":\"UPDATED\",\"data\":{\"goalName\":\"Laptop\",\"goalReached\":false}}"));
    }

    @Test void newLoanProducesMessageButEditDoesNot() throws Exception {
        assertNotNull(fmt("{\"type\":\"DEBT_CHANGED\",\"action\":\"CREATED\",\"data\":{}}"));
        assertNull(fmt("{\"type\":\"DEBT_CHANGED\",\"action\":\"UPDATED\",\"data\":{}}"));
    }

    @Test void routineExpenseIsSilent() throws Exception {
        assertNull(fmt("{\"type\":\"EXPENSE_CHANGED\",\"action\":\"CREATED\",\"data\":{\"category\":\"FOOD\"}}"));
    }

    @Test
    void duplicateEventIsPersistentlyIgnored() throws Exception {
        Set<UUID> processed = new HashSet<>();
        ProcessedEventStore processedEvents = processed::add;
        class CapturingListener extends NotificationListener {
            private int deliveries;
            CapturingListener() { super(mapper, processedEvents); }
            @Override void deliver(String userId, String text) { deliveries++; }
        }
        CapturingListener listener = new CapturingListener();
        String event = """
                {"eventId":"9a79bc76-79e7-44ba-a0b7-a7ff081f0bd5","userId":"user-1",
                 "type":"DEBT_CHANGED","action":"CREATED","data":{}}
                """;

        listener.onEvent(event);
        listener.onEvent(event);

        assertEquals(1, listener.deliveries);
        assertEquals(1, processed.size());
    }
}
