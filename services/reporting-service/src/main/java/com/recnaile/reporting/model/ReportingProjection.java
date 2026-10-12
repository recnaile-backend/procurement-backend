package com.recnaile.reporting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** Current, denormalized read model for a business entity. */
@Document("reporting_projections")
@CompoundIndex(name="projection_entity_unique", def="{'entityType':1,'entityId':1}", unique=true)
@CompoundIndex(name="projection_type_status_updated", def="{'entityType':1,'status':1,'updatedAt':-1}")
@CompoundIndex(name="projection_department_updated", def="{'departmentId':1,'updatedAt':-1}")
public record ReportingProjection(
    @Id String id,
    @Indexed String entityType,
    @Indexed String entityId,
    String status,
    String departmentId,
    String projectId,
    String itemId,
    String vendorId,
    String relatedEntityId,
    String condition,
    Double quantity,
    Double estimatedValue,
    Double orderedQuantity,
    Double acceptedQuantity,
    Double rejectedQuantity,
    Double availableQuantity,
    Double reorderThreshold,
    String actorId,
    String producer,
    Instant createdAt,
    Instant updatedAt,
    Instant sourceOccurredAt,
    String sourceEventId,
    String sourceEventType
) {}
