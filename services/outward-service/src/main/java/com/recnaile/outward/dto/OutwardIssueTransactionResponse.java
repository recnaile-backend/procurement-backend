package com.recnaile.outward.dto;

import java.time.Instant;
import java.util.List;

public record OutwardIssueTransactionResponse(
        String id,
        String outwardNumber,
        Instant issuedAt,
        List<OutwardItemResponse> items,
        String projectId,
        String requestedBy,
        String departmentName,
        String recipientEmployeeId,
        String recipientEmployeeName,
        String issuerId,
        String approverId,
        String purpose,
        boolean assetTracked,
        Long version,
        Instant createdAt) {
}
