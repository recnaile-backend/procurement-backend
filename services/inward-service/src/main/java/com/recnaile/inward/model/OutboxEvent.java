package com.recnaile.inward.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outbox_events")
@CompoundIndex(name = "outbox_status_retry_idx", def = "{'status': 1, 'nextAttemptAt': 1, 'createdAt': 1}")
public class OutboxEvent {

    public enum Status { PENDING, PUBLISHING, FAILED, PUBLISHED }

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private int eventVersion;
    private String topic;
    private String eventKey;
    private String payload;
    private Status status;
    private int attemptCount;
    private Instant nextAttemptAt;
    private Instant publishedAt;
    private String lastError;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;

    protected OutboxEvent() {}

    public OutboxEvent(
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            int eventVersion,
            String topic,
            String eventKey,
            String payload,
            Instant nextAttemptAt) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.status = Status.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public int getEventVersion() { return eventVersion; }
    public String getTopic() { return topic; }
    public String getEventKey() { return eventKey; }
    public String getPayload() { return payload; }
    public Status getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void markPublishing() {
        this.status = Status.PUBLISHING;
        this.attemptCount++;
        this.lastError = null;
    }

    public void markPublished(Instant publishedAt) {
        this.status = Status.PUBLISHED;
        this.publishedAt = publishedAt;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void markFailed(String error, Instant nextAttemptAt) {
        this.status = Status.FAILED;
        this.lastError = error;
        this.nextAttemptAt = nextAttemptAt;
    }
}
