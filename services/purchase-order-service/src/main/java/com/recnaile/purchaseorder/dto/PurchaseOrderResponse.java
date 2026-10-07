package com.recnaile.purchaseorder.dto;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.enums.PurchaseType;
import com.recnaile.purchaseorder.domain.model.ApprovedIndentSnapshot;
import com.recnaile.purchaseorder.domain.model.InvoiceMetadata;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(
        String poId,
        LocalDate poDate,
        String indentNumber,
        String indentStatus,
        ApprovedIndentResponse indentDetails,
        String vendorName,
        String projectId,
        String departmentName,
        PurchaseType purchaseType,
        List<PurchaseOrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal gstAmount,
        BigDecimal totalAmount,
        LocalDate expectedDeliveryDate,
        InvoiceMetadata invoiceMetadata,
        PurchaseOrderStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant issuedAt,
        Instant partiallyReceivedAt,
        Instant completedAt,
        Instant cancelledAt,
        String issueReason,
        String cancellationReason,
        String createdBy,
        String updatedBy,
        Long version) {

    public static PurchaseOrderResponse from(PurchaseOrder po) {
        if (po == null) {
            throw new IllegalArgumentException("Purchase Order cannot be null");
        }

        List<String> sourceIndentIds = po.getSourceIndentIds() == null
                ? List.of()
                : po.getSourceIndentIds().stream()
                        .filter(java.util.Objects::nonNull)
                        .toList();
        List<PurchaseOrderLine> lines = po.getLines() == null
                ? List.of()
                : po.getLines();

        String indentNumber = po.getIndentNumber();
        if ((indentNumber == null || indentNumber.isBlank()) && !sourceIndentIds.isEmpty()) {
            indentNumber = sourceIndentIds.get(0);
        }

        return new PurchaseOrderResponse(
                po.getPoId(),
                po.getPoDate(),
                indentNumber,
                po.getIndentStatus(),
                ApprovedIndentResponse.from(po.getIndentDetails()),
                po.getVendorName(),
                po.getProjectId(),
                po.getDepartmentName(),
                po.getPurchaseType(),
                lines.stream().filter(java.util.Objects::nonNull).map(PurchaseOrderItemResponse::from).toList(),
                po.getSubtotal(),
                po.getGstAmount(),
                po.getTotalAmount(),
                po.getExpectedDeliveryDate(),
                po.getInvoiceMetadata(),
                po.getStatus(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getIssuedAt(),
                po.getPartiallyReceivedAt(),
                po.getCompletedAt(),
                po.getCancelledAt(),
                po.getIssueReason(),
                po.getCancellationReason(),
                po.getCreatedBy(),
                po.getUpdatedBy(),
                po.getVersion());
    }

    public record ApprovedIndentResponse(
            String requesterId,
            String departmentId,
            String departmentName,
            String projectId,
            String reason,
            List<ApprovedIndentItemResponse> items,
            BigDecimal estimatedTotalAmount,
            String status,
            String approvalLevel,
            String verifierId,
            Instant verifiedAt,
            String verificationRemarks,
            String approverId,
            Instant approvedAt,
            String approvalRemarks) {

        static ApprovedIndentResponse from(ApprovedIndentSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            List<ApprovedIndentItemResponse> items = snapshot.getItems() == null
                    ? List.of()
                    : snapshot.getItems().stream()
                            .filter(java.util.Objects::nonNull)
                            .map(ApprovedIndentItemResponse::from)
                            .toList();

            return new ApprovedIndentResponse(
                    snapshot.getRequesterId(),
                    snapshot.getDepartmentId(),
                    snapshot.getDepartmentName(),
                    snapshot.getProjectId(),
                    snapshot.getReason(),
                    items,
                    snapshot.getEstimatedTotalAmount(),
                    snapshot.getStatus(),
                    snapshot.getApprovalLevel(),
                    snapshot.getVerifierId(),
                    snapshot.getVerifiedAt(),
                    snapshot.getVerificationRemarks(),
                    snapshot.getApproverId(),
                    snapshot.getApprovedAt(),
                    snapshot.getApprovalRemarks());
        }
    }

    public record ApprovedIndentItemResponse(
            String serialNumber,
            String itemName,
            BigDecimal quantity,
            BigDecimal estimatedUnitPrice,
            BigDecimal estimatedLineAmount,
            String remarks) {

        static ApprovedIndentItemResponse from(ApprovedIndentSnapshot.Item item) {
            return new ApprovedIndentItemResponse(
                    item.getItemId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getEstimatedUnitPrice(),
                    item.getEstimatedLineAmount(),
                    item.getRemarks());
        }
    }

    public record PurchaseOrderItemResponse(
            String itemId,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal gstRate) {

        static PurchaseOrderItemResponse from(PurchaseOrderLine line) {
            return new PurchaseOrderItemResponse(
                    line.getItemId(),
                    line.getItemName(),
                    line.getOrderedQuantity(),
                    line.getUnitPrice(),
                    line.getGstRate());
        }
    }
}
