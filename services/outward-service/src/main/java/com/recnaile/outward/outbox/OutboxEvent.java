package com.recnaile.outward.outbox;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outbox_events")
@CompoundIndex(name = "outbox_status_created_idx", def = "{'status': 1, 'createdAt': 1}")
public class OutboxEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    @Indexed
    private String eventType;

    @Indexed
    private String aggregateType;

    @Indexed
    private String aggregateId;

    private String payload;
    private OutboxEventStatus status;
    private Instant occurredAt;
    private Instant createdAt;
    private Instant publishedAt;
    private Instant lastAttemptAt;
    private Integer attemptCount;
    private String lastError;

    @Version
    private Long version;

    public OutboxEvent() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public OutboxEventStatus getStatus() { return status; }
    public void setStatus(OutboxEventStatus status) { this.status = status; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
