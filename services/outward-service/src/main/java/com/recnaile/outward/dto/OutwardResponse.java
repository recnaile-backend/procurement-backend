package com.recnaile.outward.dto;

import com.recnaile.outward.domain.enums.OutwardStatus;
import java.time.Instant;
import java.util.List;

public record OutwardResponse(
        String id,
        String outwardNumber,
        Instant requestedAt,
        List<OutwardItemResponse> items,
        String projectId,
        String requestedBy,
        String departmentName,
        String purpose,
        RecipientResponse recipient,
        OutwardStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt) {
}
