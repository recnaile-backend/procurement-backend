package com.recnaile.notification.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Objects;

/** Durable inbox record written before a Kafka message is acknowledged. */
@Document(collection = "notification_event_inbox")
public class IncomingDomainEvent {
    @Id
    private String id;
    @Indexed(unique = true, name = "notification_inbox_event_id_unique")
    private String eventId;
    private String eventType;
    private String topic;
    private int partition;
    private long offset;
    private Instant receivedAt;
    private Map<String, Object> envelope;
    private InboxStatus status;

    protected IncomingDomainEvent() { }

    public IncomingDomainEvent(String eventId, String eventType, String topic, int partition,
                               long offset, Instant receivedAt, Map<String, Object> envelope) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("eventId must not be blank");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType must not be blank");
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic must not be blank");
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
        this.envelope = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(envelope, "envelope")));
        this.status = InboxStatus.RECEIVED;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public int getPartition() { return partition; }
    public long getOffset() { return offset; }
    public Instant getReceivedAt() { return receivedAt; }
    public Map<String, Object> getEnvelope() { return envelope; }
    public void markProcessing() { this.status = InboxStatus.PROCESSING; }
    public void markProcessed() { this.status = InboxStatus.PROCESSED; }
    public void markFailed() { this.status = InboxStatus.FAILED; }

    public InboxStatus getStatus() { return status; }
    public enum InboxStatus { RECEIVED, PROCESSING, PROCESSED, FAILED }
}
