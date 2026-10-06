package com.recnaile.asset.event;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "processed_asset_events")
public class ProcessedEvent {

    @Id
    private String eventId;

    private String eventType;
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    private ProcessedEvent(String eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public static ProcessedEvent of(String eventId, String eventType, Instant processedAt) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (processedAt == null) {
            throw new IllegalArgumentException("Processed timestamp is required");
        }
        return new ProcessedEvent(eventId, eventType, processedAt);
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
}
