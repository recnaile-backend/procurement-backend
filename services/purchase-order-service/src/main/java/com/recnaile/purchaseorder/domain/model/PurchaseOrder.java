package com.recnaile.purchaseorder.domain.model;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.enums.PurchaseType;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "purchase_orders")
@CompoundIndex(name = "idx_vendor_name_status", def = "{'vendorName': 1, 'status': 1}")
@CompoundIndex(name = "idx_project_status", def = "{'projectId': 1, 'status': 1}")
@CompoundIndex(name = "idx_department_status", def = "{'departmentName': 1, 'status': 1}")
@CompoundIndex(name = "idx_status_created_at", def = "{'status': 1, 'createdAt': -1}")
@CompoundIndex(name = "uk_source_indent", def = "{'sourceIndentIds': 1}", unique = true)
public class PurchaseOrder {

    @Id
    private String poId;

    private LocalDate poDate;

    private List<String> sourceIndentIds = new ArrayList<>();

    private String indentNumber;
    // Snapshot of the authoritative Indent Service status at PO creation time.
    private String indentStatus;
    private String vendorId;
    private String vendorName;
    private String projectId;
    private String departmentId;
    private String departmentName;
    private PurchaseType purchaseType;

    private List<PurchaseOrderLine> lines = new ArrayList<>();

    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal gstAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private LocalDate expectedDeliveryDate;

    private InvoiceMetadata invoiceMetadata;

    // Lifecycle reasons are persisted for auditability and exposed by the PO response.
    private String issueReason;
    private String cancellationReason;

    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant issuedAt;
    private Instant partiallyReceivedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @Version
    private Long version;

    public String getPoId() { return poId; }
    public void setPoId(String poId) { this.poId = poId; }

    public LocalDate getPoDate() { return poDate; }
    public void setPoDate(LocalDate poDate) { this.poDate = poDate; }

    public List<String> getSourceIndentIds() { return sourceIndentIds; }
    public void setSourceIndentIds(List<String> sourceIndentIds) {
        this.sourceIndentIds = sourceIndentIds == null ? new ArrayList<>() : sourceIndentIds;
    }

    public String getIndentNumber() { return indentNumber; }
    public void setIndentNumber(String indentNumber) { this.indentNumber = indentNumber; }

    public String getIndentStatus() { return indentStatus; }
    public void setIndentStatus(String indentStatus) { this.indentStatus = indentStatus; }

    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public PurchaseType getPurchaseType() { return purchaseType; }
    public void setPurchaseType(PurchaseType purchaseType) { this.purchaseType = purchaseType; }

    public List<PurchaseOrderLine> getLines() { return lines; }
    public void setLines(List<PurchaseOrderLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public InvoiceMetadata getInvoiceMetadata() { return invoiceMetadata; }
    public void setInvoiceMetadata(InvoiceMetadata invoiceMetadata) { this.invoiceMetadata = invoiceMetadata; }

    public String getIssueReason() { return issueReason; }
    public void setIssueReason(String issueReason) { this.issueReason = issueReason; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public PurchaseOrderStatus getStatus() { return status; }

    /**
     * Internal lifecycle mutation. Use PurchaseOrderStateMachine.transition(...)
     * for all business status changes.
     */
    public void transitionStatus(PurchaseOrderStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getPartiallyReceivedAt() { return partiallyReceivedAt; }
    public void setPartiallyReceivedAt(Instant partiallyReceivedAt) { this.partiallyReceivedAt = partiallyReceivedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }


    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
