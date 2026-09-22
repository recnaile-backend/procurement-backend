package com.recnaile.purchaseorder.client.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record DepartmentResponse(
        String id,
        @JsonProperty("departmentName") String name,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
