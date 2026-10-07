package com.recnaile.purchaseorder.client.indent;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    public record Item(
            @JsonAlias({"serialNumber", "itemId"}) String itemId,
            String itemName,
            BigDecimal quantity,
            @JsonAlias({"estimatedUnitPrice", "unitPrice"}) BigDecimal unitPrice,
            BigDecimal gstRate,
            BigDecimal estimatedLineAmount,
            String remarks) {
    }
}
