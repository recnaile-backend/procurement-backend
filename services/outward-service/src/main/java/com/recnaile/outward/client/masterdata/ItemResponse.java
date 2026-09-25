package com.recnaile.outward.client.masterdata;

public record ItemResponse(
        String itemId,
        String name,
        String department,
        String type,
        boolean assetTracked,
        String status
) {}
