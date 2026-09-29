package com.recnaile.outward.outbox;

import java.time.Instant;

/** Immutable audit message consumed by audit-service. */
public record AuditEventPayload(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String entityType,
        String entityId,
        String action,
        String actorId,
        String actorRole,
        String correlationId,
        String sourceService,
        String beforeState,
        String afterState,
        String reason) {
}
