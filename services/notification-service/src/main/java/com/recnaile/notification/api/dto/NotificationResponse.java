package com.recnaile.notification.api.dto;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.enums.NotificationDeliveryStatus;
import com.recnaile.notification.domain.model.NotificationDocument;

import java.time.Instant;

/** Safe API representation; intentionally excludes internal failure details and raw event payloads. */
public record NotificationResponse(
        String id,
        String eventType,
        String title,
        String message,
        NotificationChannel channel,
        NotificationDeliveryStatus deliveryStatus,
        boolean read,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt,
        String correlationId,
        String traceId) {

    public static NotificationResponse from(NotificationDocument notification) {
        return new NotificationResponse(
                notification.getId(), notification.getEventType(), notification.getTitle(),
                notification.getMessage(), notification.getChannel(), notification.getDeliveryStatus(),
                notification.isRead(), notification.getReadAt(), notification.getCreatedAt(),
                notification.getUpdatedAt(), notification.getCorrelationId(), notification.getTraceId());
    }
}
