package com.recnaile.asset.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
            String outwardNumber,
            String returnId,
            AssetCondition condition,
            Instant returnedAt) {
        requireText(assetId, "Asset ID");
        String normalizedOutwardNumber = ReferenceNumberNormalizer.outwardNumber(outwardNumber);
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

        AssetAssignment activeAssignment = assignmentService.getActiveAssignment(assetId);
        if (!normalizedOutwardNumber.equals(activeAssignment.getOutwardNumber())) {
            throw new IllegalArgumentException(
                    "Outward number does not match the active asset assignment");
        }

        return assignmentService.closeAssignment(assetId, returnedAt, returnId, condition);
    }

    /**
     * Applies one Return Service RETURN_RECORDED event to the active asset
     * assignments belonging to the outward/item pair. One return request may
     * contain multiple reusable units, so exactly {@code quantity} active
     * assignments must be available.
     */
    @Transactional
    public void processReturnForOutwardItem(
            String outwardNumber,
            String itemId,
            int quantity,
            String returnId,
            AssetCondition condition,
            Instant returnedAt) {
        String normalizedOutwardNumber = ReferenceNumberNormalizer.outwardNumber(outwardNumber);
        requireText(itemId, "Item ID");
        requireText(returnId, "Return ID");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Return quantity must be positive");
        }
        if (condition != AssetCondition.GOOD && condition != AssetCondition.DAMAGED) {
            throw new IllegalArgumentException("Asset return condition must be GOOD or DAMAGED");
        }
        if (returnedAt == null) {
            throw new IllegalArgumentException("Return timestamp is required");
        }

        List<AssetAssignment> candidates = new ArrayList<>();
        for (AssetAssignment assignment : assignmentService.activeAssignmentsForOutward(normalizedOutwardNumber)) {
            Asset asset = assetService.get(assignment.getAssetId());
            if (itemId.equals(asset.getItemId())) {
                candidates.add(assignment);
            }
        }

        if (candidates.size() < quantity) {
            throw new IllegalStateException(
                    "Return quantity exceeds the active reusable assets issued for outward " + normalizedOutwardNumber
                            + " and item " + itemId);
        }

        for (int i = 0; i < quantity; i++) {
            assignmentService.closeAssignment(
                    candidates.get(i).getAssetId(), returnedAt, returnId, condition);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
