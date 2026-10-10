package com.recnaile.notification.domain.model;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.enums.NotificationDeliveryStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Persisted notification for a single recipient and channel. */
@Document(collection = "notifications")
public class NotificationDocument {
    @Id private String id;
    private String eventId;
    private String eventType;
    private String empId;
    private String recipientRole;
    private NotificationChannel channel;
    private String title;
    private String message;
    private NotificationDeliveryStatus deliveryStatus;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int deliveryAttempts;
    private Instant nextAttemptAt;
    private String failureCode;
    private String correlationId;
    private String traceId;

    protected NotificationDocument() { }

    public NotificationDocument(String eventId, String eventType, String empId,
                                 String recipientRole, NotificationChannel channel,
                                 String title, String message, String correlationId, String traceId) {
        this.eventId = requireText(eventId, "eventId");
        this.eventType = requireText(eventType, "eventType");
        this.empId = requireText(empId, "empId");
        this.recipientRole = recipientRole;
        this.channel = java.util.Objects.requireNonNull(channel, "channel");
        this.title = requireText(title, "title");
        this.message = requireText(message, "message");
        this.correlationId = correlationId;
        this.traceId = traceId;
        this.deliveryStatus = NotificationDeliveryStatus.PENDING;
        this.read = false;
        this.deliveryAttempts = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    public void markRead(Instant at) {
        if (!read) { this.read = true; this.readAt = java.util.Objects.requireNonNull(at, "at"); this.updatedAt = at; }
    }
    public void markDeliveryStatus(NotificationDeliveryStatus status, Instant at) {
        this.deliveryStatus = java.util.Objects.requireNonNull(status, "status");
        this.updatedAt = java.util.Objects.requireNonNull(at, "at");
    }
    public void recordAttempt(Instant at) { this.deliveryAttempts++; this.updatedAt = java.util.Objects.requireNonNull(at, "at"); }
    public void scheduleRetry(Instant at) { this.nextAttemptAt = at; this.deliveryStatus = NotificationDeliveryStatus.RETRYING; this.updatedAt = Instant.now(); }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; this.updatedAt = Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getEmpId() { return empId; }
    public String getRecipientRole() { return recipientRole; }
    public NotificationChannel getChannel() { return channel; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public NotificationDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public boolean isRead() { return read; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getDeliveryAttempts() { return deliveryAttempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getFailureCode() { return failureCode; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
}
