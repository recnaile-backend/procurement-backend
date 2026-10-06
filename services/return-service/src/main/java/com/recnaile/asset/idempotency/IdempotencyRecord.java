package com.recnaile.asset.idempotency;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "asset_idempotency_records")
@CompoundIndex(name = "idempotency_key_operation_unique", def = "{ 'operation': 1, 'idempotencyKey': 1 }", unique = true)
public class IdempotencyRecord {

    @Id
    private String id;
    private String operation;
    private String idempotencyKey;
    private String requestHash;
    private String status;
    private String resourceType;
    private String resourceId;
    private Instant createdAt;
    private Instant completedAt;

    protected IdempotencyRecord() { }

    private IdempotencyRecord(String id, String operation, String idempotencyKey, String requestHash,
            String status, Instant createdAt) {
        this.id = id;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static IdempotencyRecord processing(String id, String operation, String key, String requestHash, Instant now) {
        return new IdempotencyRecord(id, operation, key, requestHash, "PROCESSING", now);
    }

    public void complete(String resourceType, String resourceId, Instant now) {
        this.status = "COMPLETED";
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.completedAt = now;
    }

    public String getId() { return id; }
    public String getOperation() { return operation; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getStatus() { return status; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
