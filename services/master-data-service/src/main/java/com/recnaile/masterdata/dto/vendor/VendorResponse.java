package com.recnaile.masterdata.dto.vendor;

import java.time.Instant;

import com.recnaile.masterdata.model.MasterDataStatus;

public record VendorResponse(
        String id,
        String name,
        String email,
        String category,
        String gstNumber,
        String notes,
        String orderSource,
        String webUrl,
        MasterDataStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}