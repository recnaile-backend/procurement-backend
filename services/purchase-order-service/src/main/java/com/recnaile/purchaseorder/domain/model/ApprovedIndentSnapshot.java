package com.recnaile.purchaseorder.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable-at-creation snapshot of the approved indent data used to create the PO.
 * The Indent Service remains the source of truth; this snapshot keeps PO reads
 * self-contained and avoids a service-to-service call for every PO in a list.
 */
public class ApprovedIndentSnapshot {

    private String requesterId;
    private String departmentId;
    private String departmentName;
    private String projectId;
    private String reason;
    private List<Item> items = new ArrayList<>();
    private BigDecimal estimatedTotalAmount;
    private String status;
    private String approvalLevel;
    private String verifierId;
    private Instant verifiedAt;
    private String verificationRemarks;
    private String approverId;
    private Instant approvedAt;
    private String approvalRemarks;

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public BigDecimal getEstimatedTotalAmount() { return estimatedTotalAmount; }
    public void setEstimatedTotalAmount(BigDecimal estimatedTotalAmount) {
        this.estimatedTotalAmount = estimatedTotalAmount;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(String approvalLevel) { this.approvalLevel = approvalLevel; }

    public String getVerifierId() { return verifierId; }
    public void setVerifierId(String verifierId) { this.verifierId = verifierId; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerificationRemarks() { return verificationRemarks; }
    public void setVerificationRemarks(String verificationRemarks) {
        this.verificationRemarks = verificationRemarks;
    }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalRemarks() { return approvalRemarks; }
    public void setApprovalRemarks(String approvalRemarks) {
        this.approvalRemarks = approvalRemarks;
    }

    public static class Item {
        private String itemName;
        private BigDecimal quantity;
        private BigDecimal estimatedUnitPrice;
        private BigDecimal estimatedLineAmount;
        private String remarks;

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

        public BigDecimal getEstimatedUnitPrice() { return estimatedUnitPrice; }
        public void setEstimatedUnitPrice(BigDecimal estimatedUnitPrice) {
            this.estimatedUnitPrice = estimatedUnitPrice;
        }

        public BigDecimal getEstimatedLineAmount() { return estimatedLineAmount; }
        public void setEstimatedLineAmount(BigDecimal estimatedLineAmount) {
            this.estimatedLineAmount = estimatedLineAmount;
        }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
}
