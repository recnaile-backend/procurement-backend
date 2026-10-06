package com.recnaile.returns.model.entity;

import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.model.enums.ReturnStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Indexed(unique = true)
    private String returnId;

    @NotBlank
    @Indexed
    private String outwardId;

    @NotBlank
    @Indexed
    private String itemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotBlank
    @Indexed
    private String returnByEmpId;

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
    }

    public ReturnRequest(
            String outwardId,
            String itemId,
            Integer quantity,
            String returnByEmpId,
            ReturnCondition condition,
            String remarks,
            Instant createdAt) {
        this.outwardId = outwardId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.returnByEmpId = returnByEmpId;
        this.condition = condition;
        this.remarks = remarks;
        this.status = ReturnStatus.PENDING_VERIFICATION;
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
    public int hashCode() { return Objects.hash(id); }
}
