package com.recnaile.returns.model.entity;

import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * MongoDB aggregate root for a Return request.
 *
 * This class intentionally contains explicit business fields rather than the
 * generic payload map used by the original scaffold. Business state changes are
 * performed by the application/service layer in later phases.
 */
@Document(collection = "return_requests")
@CompoundIndex(name = "idx_return_outward_line", def = "{'outwardId': 1, 'outwardLineId': 1}")
@CompoundIndex(name = "idx_return_status_created_at", def = "{'status': 1, 'createdAt': -1}")
public class ReturnRequest {

    @Id
    private String returnId;

    @NotBlank
    @Indexed
    private String outwardId;

    /**
     * Identifies the specific issued item row inside the outward request.
     * This is distinct from itemId, which identifies the product/SKU itself.
     */
    @NotBlank
    @Indexed
    private String outwardLineId;

    @NotBlank
    @Indexed
    private String itemId;

    @NotNull
    @DecimalMin(value = "0.000001")
    private BigDecimal quantity;

    /** Optional for reusable/asset-managed items. */
    @Indexed
    private String assetId;

    @NotBlank
    @Indexed
    private String returnedBy;

    @NotNull
    private ReturnCondition condition;

    @Size(max = 2000)
    private String remarks;

    @NotNull
    @Indexed
    private ReturnStatus status;

    private String verifiedBy;

    private Instant verifiedAt;

    @NotNull
    @Indexed
    private Instant createdAt;

    @NotNull
    private Instant updatedAt;

    @Version
    private Long version;

    protected ReturnRequest() {
        // Required by Spring Data MongoDB.
    }

    public ReturnRequest(
            String outwardId,
            String outwardLineId,
            String itemId,
            BigDecimal quantity,
            String returnedBy,
            ReturnCondition condition,
            String remarks,
            String assetId,
            Instant createdAt) {
        this.returnId = UUID.randomUUID().toString();
        this.outwardId = outwardId;
        this.outwardLineId = outwardLineId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.returnedBy = returnedBy;
        this.condition = condition;
        this.remarks = remarks;
        this.assetId = assetId;
        this.status = ReturnStatus.PENDING_VERIFICATION;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getReturnId() { return returnId; }
    public String getOutwardId() { return outwardId; }
    public String getOutwardLineId() { return outwardLineId; }
    public String getItemId() { return itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public String getAssetId() { return assetId; }
    public String getReturnedBy() { return returnedBy; }
    public ReturnCondition getCondition() { return condition; }
    public String getRemarks() { return remarks; }
    public ReturnStatus getStatus() { return status; }
    public String getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setReturnId(String returnId) { this.returnId = returnId; }
    public void setOutwardId(String outwardId) { this.outwardId = outwardId; }
    public void setOutwardLineId(String outwardLineId) { this.outwardLineId = outwardLineId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public void setReturnedBy(String returnedBy) { this.returnedBy = returnedBy; }
    public void setCondition(ReturnCondition condition) { this.condition = condition; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setStatus(ReturnStatus status) { this.status = status; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReturnRequest that)) return false;
        return Objects.equals(returnId, that.returnId);
    }

    @Override
    public int hashCode() { return Objects.hash(returnId); }
}
