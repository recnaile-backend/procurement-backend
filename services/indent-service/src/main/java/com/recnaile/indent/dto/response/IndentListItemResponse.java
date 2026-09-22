package com.recnaile.indent.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import com.recnaile.indent.domain.enums.IndentStatus;

public record IndentListItemResponse(
        String id,
        String indentNumber,
        String requestBy,
        String departmentName,
        String projectId,
        Instant indentCreationDate,
        IndentStatus status,
        BigDecimal totalAmount
) {
}
