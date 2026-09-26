package com.recnaile.outward.client.masterdata;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ReferenceResponse(
        @JsonAlias({"id", "projectId"}) String id,
        @JsonAlias({"name", "departmentName", "projectName"}) String name,
        String status
) {}
