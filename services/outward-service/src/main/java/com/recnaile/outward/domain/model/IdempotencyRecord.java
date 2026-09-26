package com.recnaile.outward.domain.model;

import java.time.Instant;

import com.recnaile.outward.domain.enums.IdempotencyStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_idempotency_records")
public class IdempotencyRecord {

    @Id
    private String id;

    @Indexed(unique = true)
    private String scopeKey;

    private String idempotencyKey;
    private String endpoint;
    private String principal;
    private String requestFingerprint;
    private IdempotencyStatus status;
    private String outwardId;
    private Instant completedAt;

    @CreatedDate
    private Instant createdAt;

    public IdempotencyRecord() {}

    public IdempotencyRecord(String scopeKey, String idempotencyKey, String endpoint,
                             String principal, String requestFingerprint) {
        this.scopeKey = scopeKey;
        this.idempotencyKey = idempotencyKey;
        this.endpoint = endpoint;
        this.principal = principal;
        this.requestFingerprint = requestFingerprint;
        this.status = IdempotencyStatus.IN_PROGRESS;
    }

    public String getId() { return id; }
    public String getScopeKey() { return scopeKey; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getEndpoint() { return endpoint; }
    public String getPrincipal() { return principal; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public IdempotencyStatus getStatus() { return status; }
    public String getOutwardId() { return outwardId; }
    public Instant getCompletedAt() { return completedAt; }

    public void complete(String outwardId, Instant completedAt) {
        this.status = IdempotencyStatus.COMPLETED;
        this.outwardId = outwardId;
        this.completedAt = completedAt;
    }
}
