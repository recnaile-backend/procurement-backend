package com.recnaile.purchaseorder.client.masterdata;

import java.time.Instant;

public record ItemResponse(
        String itemId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
