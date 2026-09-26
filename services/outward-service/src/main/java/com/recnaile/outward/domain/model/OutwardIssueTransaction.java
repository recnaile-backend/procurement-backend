package com.recnaile.outward.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_issue_transactions")
public class OutwardIssueTransaction {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String outwardNumber;

    @NotNull
    private Instant issuedAt;

    @Valid
    @NotEmpty
    private List<OutwardItem> items = new ArrayList<>();

    private String projectId;

    @NotBlank
    private String requestedBy;

    @NotBlank
    private String departmentName;

    @NotBlank
    private String recipientEmployeeId;

    @NotBlank
    private String issuerId;

    @NotBlank
    private String approverId;

    @NotBlank
    private String purpose;

    private boolean assetTracked;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    public OutwardIssueTransaction() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOutwardNumber() { return outwardNumber; }
    public void setOutwardNumber(String outwardNumber) { this.outwardNumber = outwardNumber; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public List<OutwardItem> getItems() { return items; }
    public void setItems(List<OutwardItem> items) { this.items = items; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getRecipientEmployeeId() { return recipientEmployeeId; }
    public void setRecipientEmployeeId(String recipientEmployeeId) { this.recipientEmployeeId = recipientEmployeeId; }
    public String getIssuerId() { return issuerId; }
    public void setIssuerId(String issuerId) { this.issuerId = issuerId; }
    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public boolean isAssetTracked() { return assetTracked; }
    public void setAssetTracked(boolean assetTracked) { this.assetTracked = assetTracked; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
