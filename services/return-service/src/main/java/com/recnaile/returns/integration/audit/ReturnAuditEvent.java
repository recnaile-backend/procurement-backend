package com.recnaile.returns.integration.audit;

import java.time.Instant;

public record ReturnAuditEvent(
        String eventId, String eventType, String actor, String role, String entityType,
        String entityId, String action, String previousState, String newState,
        String correlationId, Instant occurredAt) {
}
