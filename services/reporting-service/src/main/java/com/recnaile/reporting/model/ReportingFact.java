package com.recnaile.reporting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** Immutable, event-keyed facts; eventId uniqueness makes event ingestion idempotent. */
@Document("reporting_event_facts")
@CompoundIndex(name="fact_type_occurred", def="{'eventType':1,'occurredAt':-1}")
@CompoundIndex(name="fact_entity_occurred", def="{'entityType':1,'entityId':1,'occurredAt':-1}")
@CompoundIndex(name="fact_ingested_at", def="{'ingestedAt':-1}")
public record ReportingFact(
    @Id String eventId,
    @Indexed String eventType,
    Instant occurredAt,
    String producer,
    String entityType,
    String entityId,
    String relatedEntityId,
    String departmentId,
    String projectId,
    String itemId,
    String vendorId,
    String status,
    String condition,
    String actorId,
    Long quantity,
    Long orderedQuantity,
    Long acceptedQuantity,
    Long rejectedQuantity,
    Long estimatedValue,
    Long unitPrice,
    Long slaWindowHours,
    Long overdueSinceHours,
    String correlationId,
    String traceId,
    Instant ingestedAt
) {
    public ReportingFact {
        requireText(eventId, "eventId");
        requireText(eventType, "eventType");
        requireText(producer, "producer");
        requireText(entityType, "entityType");
        requireText(entityId, "entityId");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt is required");
        if (ingestedAt == null) throw new IllegalArgumentException("ingestedAt is required");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
