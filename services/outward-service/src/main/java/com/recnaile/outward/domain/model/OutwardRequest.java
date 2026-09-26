package com.recnaile.outward.domain.model;

import java.time.Instant;

import com.recnaile.outward.domain.enums.OutwardStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_requests")
@CompoundIndex(name = "idx_outward_status_created", def = "{'status': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_outward_item_status", def = "{'itemId': 1, 'status': 1}")
@CompoundIndex(name = "idx_outward_recipient", def = "{'recipientUserId': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_outward_department_project", def = "{'departmentId': 1, 'projectId': 1, 'createdAt': -1}")
public class OutwardRequest {

    @Id
    private String outwardId;

    @Indexed
    private String itemId;
    private int quantity;
    private String recipientUserId;
    private String recipientName;
    private String departmentId;
    private String projectId;
    private String purpose;
    private String location;

    @Indexed
    private String requesterId;
    private String status;

    private String approverId;
    private String approverRole;
    private Instant approvedAt;
    private String rejectionReason;
    private String issuerId;
    private Instant issuedAt;
    private Instant requestedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean assetTracked;

    @Version
    private Long version;

    public OutwardRequest() {}

    public String getOutwardId() { return outwardId; }
    public void setOutwardId(String outwardId) { this.outwardId = outwardId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(String recipientUserId) { this.recipientUserId = recipientUserId; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }
    public String getApproverRole() { return approverRole; }
    public void setApproverRole(String approverRole) { this.approverRole = approverRole; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getIssuerId() { return issuerId; }
    public void setIssuerId(String issuerId) { this.issuerId = issuerId; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isAssetTracked() { return assetTracked; }
    public void setAssetTracked(boolean assetTracked) { this.assetTracked = assetTracked; }
    public Long getVersion() { return version; }
}
