package com.recnaile.outward.client.masterdata;

import java.time.Instant;

public record ProjectResponse(
        String projectId,
        String projectName,
        String teamLeadId,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
