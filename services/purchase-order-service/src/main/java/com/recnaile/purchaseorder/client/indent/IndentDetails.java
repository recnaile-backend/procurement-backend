package com.recnaile.purchaseorder.client.indent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record IndentDetails(
        String indentNumber,
        String requesterId,
        String departmentId,
        String departmentName,
        String projectId,
        String reason,
        List<Item> items,
        BigDecimal estimatedTotalAmount,
        String status,
        String approvalLevel,
        String verifierId,
        Instant verifiedAt,
        String verificationRemarks,
        String approverId,
        Instant approvedAt,
        String approvalRemarks) {

    /**
     * Approved indent item data used to create the PO. Legacy serial/item identifiers
     * supplied by older indent payloads are intentionally not promoted into the PO.
     */
    public record Item(
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal gstRate,
            BigDecimal estimatedLineAmount,
            String remarks) {
    }
}
