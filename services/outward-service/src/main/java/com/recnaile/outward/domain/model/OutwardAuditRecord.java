package com.recnaile.outward.domain.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_audit_records")
@CompoundIndex(name = "idx_audit_entity_time", def = "{'entityId': 1, 'occurredAt': -1}")
public class OutwardAuditRecord {

    @Id
    private String id;
    private String entityType;
    private String entityId;
    private String action;
    private String actorId;
    private String actorRole;
    private Instant occurredAt;
    private String correlationId;
    private String traceId;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;

    public OutwardAuditRecord() {}

    public OutwardAuditRecord(String id, String entityType, String entityId, String action,
                              String actorId, String actorRole, Instant occurredAt,
                              String correlationId, String traceId,
                              Map<String, Object> beforeState, Map<String, Object> afterState) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.traceId = traceId;
        this.beforeState = beforeState;
        this.afterState = afterState;
    }

    public String getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public Map<String, Object> getBeforeState() { return beforeState; }
    public Map<String, Object> getAfterState() { return afterState; }
}
