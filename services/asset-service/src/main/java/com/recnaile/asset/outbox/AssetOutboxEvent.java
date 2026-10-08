package com.recnaile.asset.outbox;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Durable Kafka publication record written in the same Mongo transaction as the business mutation. */
@Document(collection = "asset_outbox_events")
public class AssetOutboxEvent {
    public enum Status { PENDING, PUBLISHING, PUBLISHED, FAILED }

    @Id
    private String eventId;
    private String topic;
    private String messageKey;
    private String payload;
    private Status status;
    private int attempts;
    private Instant createdAt;
    private Instant nextAttemptAt;
    private Instant lockedUntil;
    private Instant publishedAt;
    private String lastError;

    protected AssetOutboxEvent() {}

    private AssetOutboxEvent(String eventId, String topic, String messageKey, String payload, Instant now) {
        this.eventId = eventId;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.status = Status.PENDING;
        this.attempts = 0;
        this.createdAt = now;
        this.nextAttemptAt = now;
    }

    public static AssetOutboxEvent pending(String eventId, String topic, String messageKey, String payload, Instant now) {
        return new AssetOutboxEvent(eventId, topic, messageKey, payload, now);
    }

    public void markPublishing(Instant until) {
        this.status = Status.PUBLISHING;
        this.attempts++;
        this.lockedUntil = until;
        this.lastError = null;
    }

    public void markPublished(Instant now) {
        this.status = Status.PUBLISHED;
        this.publishedAt = now;
        this.lockedUntil = null;
        this.lastError = null;
    }

    public void markFailed(String error, Instant nextAttempt) {
        this.status = Status.FAILED;
        this.lastError = error == null ? "Kafka publication failed" : error.substring(0, Math.min(error.length(), 2000));
        this.nextAttemptAt = nextAttempt;
        this.lockedUntil = null;
    }

    public String getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public String getMessageKey() { return messageKey; }
    public String getPayload() { return payload; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getLastError() { return lastError; }
}
