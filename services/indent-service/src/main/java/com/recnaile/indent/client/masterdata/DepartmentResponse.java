package com.recnaile.indent.client.masterdata;

import java.time.Instant;

public record DepartmentResponse(
        String id,
        String departmentName,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
