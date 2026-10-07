package com.finplan.notification;

import java.util.UUID;

public interface ProcessedEventStore {
    boolean markIfNew(UUID eventId);
}
