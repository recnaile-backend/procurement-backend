package com.recnaile.masterdata.dto.project;

import java.time.Instant;

import com.recnaile.masterdata.model.MasterDataStatus;

public record ProjectResponse(
        String projectId,
        String projectName,
        String teamLeadId,
        MasterDataStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}