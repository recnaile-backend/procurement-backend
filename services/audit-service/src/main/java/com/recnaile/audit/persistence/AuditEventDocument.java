package com.recnaile.audit.persistence;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.model.AuditEventPayload;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Append-only MongoDB representation of a platform audit event. */
@Document(collection = "audit_events")
@CompoundIndexes({
    @CompoundIndex(name = "idx_audit_entity_history", def = "{'entityType': 1, 'entityId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_audit_actor_history", def = "{'actorId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_audit_service_action_time", def = "{'service': 1, 'action': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_audit_correlation", def = "{'correlationId': 1}", sparse = true),
    @CompoundIndex(name = "idx_audit_trace", def = "{'traceId': 1}", sparse = true)
})
public record AuditEventDocument(
    @Id UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String producer,
    String correlationId,
    String traceId,
    String actorId,
    List<String> actorRoles,
    String service,
    String entityType,
    String entityId,
    String action,
    Instant timestamp,
    String beforeStatus,
    String afterStatus,
    String poId,
    String deliveryNoteNumber,
    String receivedBy,
    String integrityHash
) {
    /** Compatibility constructor for existing tests/callers; new events should use from(event). */
    public AuditEventDocument(UUID eventId, String eventType, int eventVersion, Instant occurredAt,
            String producer, String correlationId, String traceId, String actorId, List<String> actorRoles,
            String service, String entityType, String entityId, String action, Instant timestamp,
            String beforeStatus, String afterStatus, String poId, String deliveryNoteNumber, String receivedBy) {
        this(eventId, eventType, eventVersion, occurredAt, producer, correlationId, traceId, actorId,
                actorRoles, service, entityType, entityId, action, timestamp, beforeStatus, afterStatus,
                poId, deliveryNoteNumber, receivedBy, null);
    }

    public static AuditEventDocument from(AuditEvent event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("Audit event and payload are required");
        }
        AuditEventPayload payload = event.payload();
        AuditEventDocument mapped = new AuditEventDocument(
            event.eventId(), event.eventType(), event.eventVersion(), event.occurredAt(),
            event.producer(), event.correlationId(), event.traceId(), payload.actorId(),
            payload.actorRoles(), payload.service(), payload.entityType(), payload.entityId(),
            payload.action(), payload.timestamp(), payload.beforeStatus(), payload.afterStatus(),
            payload.poId(), payload.deliveryNoteNumber(), payload.receivedBy(), null
        );
        return new AuditEventDocument(
            mapped.eventId(), mapped.eventType(), mapped.eventVersion(), mapped.occurredAt(), mapped.producer(),
            mapped.correlationId(), mapped.traceId(), mapped.actorId(), mapped.actorRoles(), mapped.service(),
            mapped.entityType(), mapped.entityId(), mapped.action(), mapped.timestamp(), mapped.beforeStatus(),
            mapped.afterStatus(), mapped.poId(), mapped.deliveryNoteNumber(), mapped.receivedBy(),
            AuditEventIntegrity.calculate(mapped)
        );
    }
}
