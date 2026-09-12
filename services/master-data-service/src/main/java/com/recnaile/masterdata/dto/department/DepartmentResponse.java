package com.recnaile.masterdata.dto.department;

import java.time.Instant;

import com.recnaile.masterdata.model.MasterDataStatus;

public record DepartmentResponse(
        String id,
        String name,
        String description,
        MasterDataStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}