package com.recnaile.asset.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.model.Asset;

/**
 * Coordinates reusable-asset return processing. Stock restoration remains the
 * responsibility of Inventory Service; this service only closes the asset
 * assignment and applies the asset lifecycle outcome.
 */
@Service
public class AssetReturnService {

    private final AssetService assetService;
    private final AssetAssignmentService assignmentService;

    public AssetReturnService(AssetService assetService, AssetAssignmentService assignmentService) {
        this.assetService = assetService;
        this.assignmentService = assignmentService;
    }

    @Transactional
    public AssetAssignment processReturn(
            String assetId,
            String returnId,
            AssetCondition condition,
            Instant returnedAt) {
        requireText(assetId, "Asset ID");
        requireText(returnId, "Return ID");
        if (condition != AssetCondition.GOOD && condition != AssetCondition.DAMAGED) {
            throw new IllegalArgumentException("Asset return condition must be GOOD or DAMAGED");
        }
        if (returnedAt == null) {
            throw new IllegalArgumentException("Return timestamp is required");
        }

        Asset asset = assetService.get(assetId);
        if (asset.getStatus() != AssetStatus.IN_USE || asset.getAssignedUserId() == null) {
            throw new IllegalStateException("Only an assigned IN_USE asset can be returned");
        }

        return assignmentService.closeAssignment(assetId, returnedAt, returnId, condition);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
