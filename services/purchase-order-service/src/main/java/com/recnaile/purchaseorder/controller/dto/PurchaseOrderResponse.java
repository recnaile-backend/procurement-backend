package com.recnaile.purchaseorder.controller.dto;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.enums.PurchaseType;
import com.recnaile.purchaseorder.domain.model.InvoiceMetadata;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(
        String poId,
        String poNumber,
        LocalDate poDate,
        List<String> sourceIndentIds,
        String vendorId,
        String projectId,
        String departmentId,
        PurchaseType purchaseType,
        List<PurchaseOrderLineResponse> lines,
        BigDecimal subtotal,
        BigDecimal gstAmount,
        BigDecimal totalAmount,
        LocalDate expectedDeliveryDate,
        InvoiceMetadata invoiceMetadata,
        String remarks,
        PurchaseOrderStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant issuedAt,
        Instant partiallyReceivedAt,
        Instant completedAt,
        Instant closedAt,
        Instant cancelledAt,
        String createdBy,
        String updatedBy,
        Long version) {

    public static PurchaseOrderResponse from(PurchaseOrder po) {
        return new PurchaseOrderResponse(
                po.getPoId(),
                po.getPoNumber(),
                po.getPoDate(),
                po.getSourceIndentIds(),
                po.getVendorId(),
                po.getProjectId(),
                po.getDepartmentId(),
                po.getPurchaseType(),
                po.getLines().stream().map(PurchaseOrderLineResponse::from).toList(),
                po.getSubtotal(),
                po.getGstAmount(),
                po.getTotalAmount(),
                po.getExpectedDeliveryDate(),
                po.getInvoiceMetadata(),
                po.getRemarks(),
                po.getStatus(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getIssuedAt(),
                po.getPartiallyReceivedAt(),
                po.getCompletedAt(),
                po.getClosedAt(),
                po.getCancelledAt(),
                po.getCreatedBy(),
                po.getUpdatedBy(),
                po.getVersion());
    }

    public record PurchaseOrderLineResponse(
            String itemId,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal remainingQuantity,
            BigDecimal unitPrice,
            BigDecimal gstRate,
            BigDecimal lineTotal) {

        static PurchaseOrderLineResponse from(PurchaseOrderLine line) {
            return new PurchaseOrderLineResponse(
                    line.getItemId(),
                    line.getItemName(),
                    line.getOrderedQuantity(),
                    line.getReceivedQuantity(),
                    line.getRemainingQuantity(),
                    line.getUnitPrice(),
                    line.getGstRate(),
                    line.getLineTotal());
        }
    }
}
