package com.recnaile.inventory.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventory_adjustment_idempotency")
public class ManualAdjustmentIdempotencyDocument {
    @Id
    private String id;
    @Indexed(unique = true)
    private String idempotencyKey;
    private String fingerprint;
    private String balanceKey;
    private int targetAvailable;
    private Instant createdAt;

    public ManualAdjustmentIdempotencyDocument() {}

    public ManualAdjustmentIdempotencyDocument(String idempotencyKey, String fingerprint,
                                               String balanceKey, int targetAvailable, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.fingerprint = fingerprint;
        this.balanceKey = balanceKey;
        this.targetAvailable = targetAvailable;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFingerprint() { return fingerprint; }
    public String getBalanceKey() { return balanceKey; }
    public int getTargetAvailable() { return targetAvailable; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
}
