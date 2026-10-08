package com.recnaile.returns.outbox;

import com.recnaile.returns.model.enums.OutboxStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "return_event_outbox")
@CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': 1}")
public class ReturnEventOutbox {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    @Indexed
    private String aggregateId;

    private String eventType;
    private String topic;
    private String returnId;
    private String outwardId;
    private String itemId;
    private Integer qty;
    private String condition;
    private String payload;
    private Instant occurredAt;
    private OutboxStatus status;
    private int attempts;
    private Instant createdAt;
    private Instant publishedAt;
    private String lastError;

    @Version
    private Long version;

    protected ReturnEventOutbox() {
    }

    public ReturnEventOutbox(
            String eventId,
            String aggregateId,
            String eventType,
            String topic,
            String returnId,
            String outwardId,
            String itemId,
            Integer qty,
            String condition,
            String payload,
            Instant occurredAt,
            Instant createdAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.returnId = returnId;
        this.outwardId = outwardId;
        this.itemId = itemId;
        this.qty = qty;
        this.condition = condition;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getReturnId() { return returnId; }
    public String getOutwardId() { return outwardId; }
    public String getItemId() { return itemId; }
    public Integer getQty() { return qty; }
    public String getCondition() { return condition; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public OutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getLastError() { return lastError; }
    public Long getVersion() { return version; }

    public void markSent(Instant publishedAt) {
        this.status = OutboxStatus.SENT;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markAttemptFailed(String error) {
        this.attempts++;
        this.lastError = error;
        this.status = OutboxStatus.PENDING;
    }
}
