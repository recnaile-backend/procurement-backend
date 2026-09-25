package com.recnaile.outward.dto.response;

import java.time.Instant;

public record OutwardResponse(
        String outwardId,
        String itemId,
        int quantity,
        String recipientUserId,
        String recipientName,
        String departmentId,
        String projectId,
        String purpose,
        String location,
        String requesterId,
        String status,
        String approverId,
        String approverRole,
        Instant approvedAt,
        String rejectionReason,
        String issuerId,
        Instant issuedAt,
        Instant requestedAt,
        boolean assetTracked,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
