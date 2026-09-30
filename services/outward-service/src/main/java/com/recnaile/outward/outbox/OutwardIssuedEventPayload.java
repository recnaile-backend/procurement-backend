package com.recnaile.outward.outbox;

import com.recnaile.outward.domain.model.OutwardItem;
import java.time.Instant;
import java.util.List;

public record OutwardIssuedEventPayload(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String outwardNumber,
        String requestedBy,
        String issuerId,
        String approverId,
        String departmentName,
        String projectId,
        String recipientEmployeeId,
        String recipientEmployeeName,
        String purpose,
        boolean assetTracked,
        List<OutwardItem> items) {
}
