package com.recnaile.inventory.domain;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventory_audit")
public class InventoryAuditDocument {
    @Id private String id;
    private String entityType; private String entityId; private String action; private String eventId;
    private String actorId; private String role; private Instant createdAt; private String correlationId;
    private String traceId; private Map<String,Object> beforeState; private Map<String,Object> afterState;
    public InventoryAuditDocument(){}
    public InventoryAuditDocument(String entityType,String entityId,String action,String eventId,String actorId,String role,
        Instant createdAt,String correlationId,String traceId,Map<String,Object> beforeState,Map<String,Object> afterState){
        this.entityType=entityType;this.entityId=entityId;this.action=action;this.eventId=eventId;this.actorId=actorId;
        this.role=role;this.createdAt=createdAt;this.correlationId=correlationId;this.traceId=traceId;
        this.beforeState=beforeState;this.afterState=afterState;
    }
    public String getId(){return id;}
}
