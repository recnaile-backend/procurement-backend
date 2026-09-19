package com.recnaile.purchaseorder.event;

import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderEventPayload(
        String poId,
        String poNumber,
        LocalDate poDate,
        List<String> sourceIndentIds,
        String vendorId,
        String projectId,
        String departmentId,
        String purchaseType,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal gstAmount,
        BigDecimal totalAmount,
        LocalDate expectedDeliveryDate,
        String status,
        String remarks,
        Long version) {

    public static PurchaseOrderEventPayload from(PurchaseOrder po) {
        return new PurchaseOrderEventPayload(
                po.getPoId(),
                po.getPoNumber(),
                po.getPoDate(),
                List.copyOf(po.getSourceIndentIds()),
                po.getVendorId(),
                po.getProjectId(),
                po.getDepartmentId(),
                po.getPurchaseType() == null ? null : po.getPurchaseType().name(),
                po.getLines().stream().map(Line::from).toList(),
                po.getSubtotal(),
                po.getGstAmount(),
                po.getTotalAmount(),
                po.getExpectedDeliveryDate(),
                po.getStatus() == null ? null : po.getStatus().name(),
                po.getRemarks(),
                po.getVersion());
    }

    public record Line(
            String itemId,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal remainingQuantity,
            BigDecimal unitPrice,
            BigDecimal gstRate,
            BigDecimal lineSubtotal,
            BigDecimal gstAmount,
            BigDecimal lineTotal) {

        static Line from(PurchaseOrderLine line) {
            return new Line(
                    line.getItemId(),
                    line.getItemName(),
                    line.getOrderedQuantity(),
                    line.getReceivedQuantity(),
                    line.getRemainingQuantity(),
                    line.getUnitPrice(),
                    line.getGstRate(),
                    line.getLineSubtotal(),
                    line.getGstAmount(),
                    line.getLineTotal());
        }
    }
}
