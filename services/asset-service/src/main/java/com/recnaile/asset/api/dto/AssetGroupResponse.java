package com.recnaile.asset.api.dto;

import java.util.List;

/**
 * Logical view of the physical assets created for one outward/item allocation.
 * Individual Asset documents remain the source of truth; this DTO is only a
 * read-side grouping for a compact operational view.
 */
public record AssetGroupResponse(
        String outwardNumber,
        String itemId,
        int totalAssets,
        int inUse,
        int returned,
        int underRepair,
        int retired,
        List<String> assignedUserIds) {
}
