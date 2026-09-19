package com.recnaile.purchaseorder.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PurchaseOrderEventEnvelope(
        String eventId,
        String eventType,
        String eventVersion,
        Instant occurredAt,
        String producerService,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String causationId,
        String actorId,
        String actorRole,
        String schemaVersion,
        Object payload) {
}
