package com.recnaile.indent.client.masterdata;

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
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
