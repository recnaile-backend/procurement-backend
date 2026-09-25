package com.recnaile.outward.event;

import java.time.Instant;

public record OutwardEventEnvelope(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String traceId,
        String actorId,
        String actorRole,
        int schemaVersion,
        OutwardEventPayload payload
) {}
