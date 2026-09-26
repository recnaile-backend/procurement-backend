package com.recnaile.outward.domain.model;

import java.time.Instant;

import com.recnaile.outward.domain.enums.OutboxStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_outbox_events")
@CompoundIndex(name = "idx_outbox_status_retry", def = "{'status': 1, 'nextAttemptAt': 1, 'createdAt': 1}")
public class OutboxEvent {

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
    private OutboxStatus status;
    private int attemptCount;
    private Instant nextAttemptAt;
    private Instant publishedAt;
    private String lastError;

    @CreatedDate
    private Instant createdAt;

    @Version
    private Long version;

    public OutboxEvent() {}

    public OutboxEvent(String eventId, String aggregateType, String aggregateId, String eventType,
                        int eventVersion, String topic, String eventKey, String payload, Instant nextAttemptAt) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
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
    public OutboxStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public void markPublished(Instant at) {
        status = OutboxStatus.PUBLISHED;
        publishedAt = at;
        nextAttemptAt = null;
        lastError = null;
    }

    public void markFailed(String error, Instant retryAt) {
        status = OutboxStatus.FAILED;
        lastError = error;
        nextAttemptAt = retryAt;
    }
}
