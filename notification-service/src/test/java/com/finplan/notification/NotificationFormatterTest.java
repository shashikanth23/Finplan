package com.finplan.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
}
