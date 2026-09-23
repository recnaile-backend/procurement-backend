package com.recnaile.indent.domain.model;

import com.recnaile.indent.domain.enums.ApprovalLevel;
import com.recnaile.indent.domain.enums.IndentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "indents")
@CompoundIndex(
        name = "idx_indent_status_approval_due_sla",
        def = "{'status': 1, 'approvalDueAt': 1, 'slaBreachAt': 1}"
)
@CompoundIndex(
        name = "idx_indent_department_status_created",
        def = "{'departmentId': 1, 'status': 1, 'createdAt': -1}"
)
public class Indent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String indentNumber;

    private String requesterId;
    private String departmentId;
    private String departmentName;
    private String projectId;
    private String reason;

    private String remarks;

    private List<IndentItem> items = new ArrayList<>();

    private BigDecimal estimatedTotalAmount;
    private IndentStatus status;
    private ApprovalLevel approvalLevel;

    private String verifierId;
    private Instant verifiedAt;
    private String verificationRemarks;

    private String approverId;
    private Instant approvedAt;
    private String approvalRemarks;

    private String rejectionReason;
    private String rejectionActorId;
    private Instant rejectedAt;

    private Instant indentCreationDate;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant updatedAt;
    private Instant approvalDueAt;
    private Instant slaBreachAt;

    @Version
    private long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<IndentItem> getItems() { return items; }
    public void setItems(List<IndentItem> items) { this.items = items; }

    public BigDecimal getEstimatedTotalAmount() { return estimatedTotalAmount; }
    public void setEstimatedTotalAmount(BigDecimal estimatedTotalAmount) {
        this.estimatedTotalAmount = estimatedTotalAmount;
    }

    public IndentStatus getStatus() { return status; }
    public void setStatus(IndentStatus status) { this.status = status; }

    public ApprovalLevel getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(ApprovalLevel approvalLevel) { this.approvalLevel = approvalLevel; }

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
    public void setApprovalRemarks(String approvalRemarks) { this.approvalRemarks = approvalRemarks; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRejectionActorId() { return rejectionActorId; }
    public void setRejectionActorId(String rejectionActorId) { this.rejectionActorId = rejectionActorId; }

    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getApprovalDueAt() { return approvalDueAt; }
    public void setApprovalDueAt(Instant approvalDueAt) { this.approvalDueAt = approvalDueAt; }

    public Instant getSlaBreachAt() { return slaBreachAt; }
    public void setSlaBreachAt(Instant slaBreachAt) { this.slaBreachAt = slaBreachAt; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getIndentNumber() {
        return indentNumber;
    }

    public void setIndentNumber(String indentNumber) {
        this.indentNumber = indentNumber;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getIndentCreationDate() {
        return indentCreationDate;
    }

    public void setIndentCreationDate(Instant indentCreationDate) {
        this.indentCreationDate = indentCreationDate;
    }

}
