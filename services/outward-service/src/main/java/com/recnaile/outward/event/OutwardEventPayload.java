package com.recnaile.outward.event;

public record OutwardEventPayload(
        String outwardId,
        String issueTransactionId,
        String itemId,
        int quantity,
        int qty,
        String recipientUserId,
        String userId,
        String recipientName,
        String requesterId,
        String issuerId,
        String receivedBy,
        String approverId,
        String departmentId,
        String projectId,
        String purpose,
        String location,
        boolean reusable,
        String actionReason
) {}
