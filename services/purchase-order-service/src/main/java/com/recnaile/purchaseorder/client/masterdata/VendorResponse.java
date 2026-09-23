package com.recnaile.purchaseorder.client.masterdata;

import java.time.Instant;

public record VendorResponse(
        String id,
        String name,
        String email,
        String category,
        String gstNumber,
        String notes,
        String orderSource,
        String webUrl,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
