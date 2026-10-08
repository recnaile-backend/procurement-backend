package com.recnaile.returns.model.entity;

import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;

/** MongoDB aggregate root for a Return request. */
@Document(collection = "return_requests")
@CompoundIndex(name = "idx_return_outward_item", def = "{'outwardId': 1, 'itemId': 1}")
@CompoundIndex(name = "idx_return_status_created_at", def = "{'status': 1, 'createdAt': -1}")
public class ReturnRequest {

    @Id
    private String id;

    @Indexed(unique = true)
    private String returnId;

    @Indexed
    private String outwardId;

    @Indexed
    private String itemId;

    private Integer quantity;

    @Indexed
    private String returnByEmpId;

    private ReturnCondition condition;

    private String remarks;

    @Indexed
    private ReturnStatus status;

    /**
     * Snapshot of the Outward/Item reusable flag at creation time.
     * This prevents Return Service from sending an Asset event for consumable stock.
     */
    @Indexed
    private Boolean assetTracked;

    private String verifiedBy;
    private Instant verifiedAt;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;

    protected ReturnRequest() {
    }

    public ReturnRequest(
            String outwardId,
            String itemId,
            Integer quantity,
            String returnByEmpId,
            ReturnCondition condition,
            String remarks,
            Instant createdAt) {
        this(outwardId, itemId, quantity, returnByEmpId, condition, remarks, createdAt, false);
    }

    public ReturnRequest(
            String outwardId,
            String itemId,
            Integer quantity,
            String returnByEmpId,
            ReturnCondition condition,
            String remarks,
            Instant createdAt,
            Boolean assetTracked) {
        this.outwardId = outwardId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.returnByEmpId = returnByEmpId;
        this.condition = condition;
        this.remarks = remarks;
        this.status = ReturnStatus.PENDING_VERIFICATION;
        this.assetTracked = Boolean.TRUE.equals(assetTracked);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() { return id; }
    public String getReturnId() { return returnId; }
    public String getOutwardId() { return outwardId; }
    public String getItemId() { return itemId; }
    public Integer getQuantity() { return quantity; }
    public String getReturnByEmpId() { return returnByEmpId; }
    public ReturnCondition getCondition() { return condition; }
    public String getRemarks() { return remarks; }
    public ReturnStatus getStatus() { return status; }
    public Boolean getAssetTracked() { return assetTracked; }
    public String getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setId(String id) { this.id = id; }
    public void setReturnId(String returnId) { this.returnId = returnId; }
    public void setOutwardId(String outwardId) { this.outwardId = outwardId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setReturnByEmpId(String returnByEmpId) { this.returnByEmpId = returnByEmpId; }
    public void setCondition(ReturnCondition condition) { this.condition = condition; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setStatus(ReturnStatus status) { this.status = status; }
    public void setAssetTracked(Boolean assetTracked) { this.assetTracked = assetTracked; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReturnRequest that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
