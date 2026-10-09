package com.recnaile.notification.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;

/** Durable idempotency ledger for a source event. */
@Document(collection = "notification_event_processing")
public class EventProcessingRecord {
    @Id
    private String id;

    @Indexed(unique = true, name = "notification_event_id_unique")
    private String eventId;

    private String eventType;
    private ProcessingStatus status;
    private Instant firstSeenAt;
    private Instant updatedAt;
    private String failureCode;

    protected EventProcessingRecord() { }

    public EventProcessingRecord(String eventId, String eventType, Instant now) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("eventId must not be blank");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType must not be blank");
        this.eventId = eventId;
        this.eventType = eventType;
        this.status = ProcessingStatus.RECEIVED;
        this.firstSeenAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public void markProcessed(Instant now) {
        this.status = ProcessingStatus.PROCESSED;
        this.updatedAt = Objects.requireNonNull(now, "now");
        this.failureCode = null;
    }

    public void markFailed(String safeFailureCode, Instant now) {
        this.status = ProcessingStatus.FAILED;
        this.updatedAt = Objects.requireNonNull(now, "now");
        this.failureCode = safeFailureCode;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public ProcessingStatus getStatus() { return status; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getFailureCode() { return failureCode; }

    public enum ProcessingStatus { RECEIVED, PROCESSED, FAILED }
}
