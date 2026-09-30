package com.recnaile.returns.idempotency;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "return_idempotency_records")
public class IdempotencyRecord {
    @Id
    private String idempotencyKey;
    @Indexed
    private String operation;
    @Indexed
    private String actor;
    private String requestHash;
    private String returnId;
    private Instant createdAt;
    @Version
    private Long version;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String operation, String actor,
                             String requestHash, String returnId, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.actor = actor;
        this.requestHash = requestHash;
        this.returnId = returnId;
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getOperation() { return operation; }
    public String getActor() { return actor; }
    public String getRequestHash() { return requestHash; }
    public String getReturnId() { return returnId; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
    public void setReturnId(String returnId) { this.returnId = returnId; }
    public void setVersion(Long version) { this.version = version; }
}
