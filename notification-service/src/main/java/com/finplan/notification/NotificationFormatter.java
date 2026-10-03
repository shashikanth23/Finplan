package com.finplan.notification;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/** Decides which finance events deserve a message to the user. Pure logic, no I/O. */
public final class NotificationFormatter {
    private NotificationFormatter() {}

    public static Optional<String> format(JsonNode event) {
        String type = event.path("type").asText("");
        String action = event.path("action").asText("");
        JsonNode data = event.path("data");

        return switch (type) {
            case "GOAL_CHANGED" -> !"DELETED".equals(action) && data.path("goalReached").asBoolean(false)
                    ? Optional.of("Goal reached: " + data.path("goalName").asText("your goal") + ". Well done!")
                    : Optional.empty();
            case "DEBT_CHANGED" -> "CREATED".equals(action)
                    ? Optional.of("A new loan was added. Check your EMI burden and financial health on the dashboard.")
                    : Optional.empty();
            case "PROFILE_CHANGED" ->
                    Optional.of("Your tax settings changed (" + data.path("taxRegime").asText("?")
                            + " regime). Your required salary was recalculated.");
            default -> Optional.empty();   // routine income/expense edits are not worth a message
        };
    }
}
