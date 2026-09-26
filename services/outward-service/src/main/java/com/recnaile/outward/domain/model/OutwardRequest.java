package com.recnaile.outward.domain.model;

import com.recnaile.outward.domain.enums.OutwardStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_requests")
@CompoundIndex(name = "outward_status_requested_at_idx", def = "{'status': 1, 'requestedAt': -1}")
public class OutwardRequest {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String outwardNumber;

    @NotNull
    private Instant requestedAt;

    @Valid
    @NotEmpty
    private List<OutwardItem> items = new ArrayList<>();

    private String projectId;

    @NotBlank
    @Indexed
    private String requestedBy;

    @NotBlank
    @Indexed
    private String departmentName;

    @NotBlank
    private String purpose;

    @Valid
    @NotNull
    private RecipientDetails recipient;

    private String approvedBy;

    private boolean assetTracked;

    @NotNull
    private OutwardStatus status;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public OutwardRequest() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOutwardNumber() { return outwardNumber; }
    public void setOutwardNumber(String outwardNumber) { this.outwardNumber = outwardNumber; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public List<OutwardItem> getItems() { return items; }
    public void setItems(List<OutwardItem> items) { this.items = items; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public RecipientDetails getRecipient() { return recipient; }
    public void setRecipient(RecipientDetails recipient) { this.recipient = recipient; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public boolean isAssetTracked() { return assetTracked; }
    public void setAssetTracked(boolean assetTracked) { this.assetTracked = assetTracked; }
    public OutwardStatus getStatus() { return status; }
    public void setStatus(OutwardStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
