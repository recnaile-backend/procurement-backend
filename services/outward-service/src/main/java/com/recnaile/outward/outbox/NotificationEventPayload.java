package com.recnaile.outward.outbox;

import java.time.Instant;

/** Notification hook emitted for downstream notification-service consumption. */
public record NotificationEventPayload(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String notificationType,
        String recipientId,
        String recipientRole,
        String entityType,
        String entityId,
        String action,
        String title,
        String message,
        String correlationId,
        String sourceService) {
}
