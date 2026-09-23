package com.recnaile.indent.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.recnaile.indent.domain.enums.IndentStatus;

public record IndentDetailsResponse(
        String id,
        String indentNumber,
        String requestBy,
        String departmentName,
        String projectId,
        String reason,
        String remarks,
        Instant indentCreationDate,
        IndentStatus status,
        List<IndentItemResponse> items,
        BigDecimal totalAmount
) {
}
