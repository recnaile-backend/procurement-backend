package com.recnaile.outward.client.masterdata;

import java.time.Instant;

public record DepartmentResponse(
        String id,
        String departmentName,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
