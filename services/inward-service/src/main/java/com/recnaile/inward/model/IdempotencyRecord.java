package com.recnaile.inward.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "grn_idempotency_records")
public class IdempotencyRecord {

    public enum Status { IN_PROGRESS, COMPLETED }

    @Id
    private String id;

    @Indexed(unique = true)
    private String scopeKey;

    private String idempotencyKey;
    private String endpoint;
    private String principal;
    private String requestFingerprint;
    private Status status;
    private String grnId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String scopeKey, String idempotencyKey, String endpoint,
                             String principal, String requestFingerprint) {
        this.scopeKey = scopeKey;
        this.idempotencyKey = idempotencyKey;
        this.endpoint = endpoint;
        this.principal = principal;
        this.requestFingerprint = requestFingerprint;
        this.status = Status.IN_PROGRESS;
    }

    public String getScopeKey() { return scopeKey; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getEndpoint() { return endpoint; }
    public String getPrincipal() { return principal; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public Status getStatus() { return status; }
    public String getGrnId() { return grnId; }
    public void complete(String grnId) { this.status = Status.COMPLETED; this.grnId = grnId; }
}
