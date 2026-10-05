package com.recnaile.returns.integration.notification;

import java.time.Instant;

public record ReturnNotificationEvent(
        String eventId, String eventType, String recipient, String returnId,
        String action, String message, String correlationId, Instant occurredAt) {
}
