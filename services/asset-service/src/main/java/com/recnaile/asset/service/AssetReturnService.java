package com.recnaile.asset.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.recnaile.asset.config.KafkaEventProperties;
import com.recnaile.asset.outbox.AssetOutboxService;

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
    private final AssetOutboxService outboxService;
    private final KafkaEventProperties kafkaProperties;

    @Autowired
    public AssetReturnService(AssetService assetService, AssetAssignmentService assignmentService,
            AssetOutboxService outboxService, KafkaEventProperties kafkaProperties) {
        this.assetService = assetService;
        this.assignmentService = assignmentService;
        this.outboxService = outboxService;
        this.kafkaProperties = kafkaProperties;
    }

    /** Test-friendly constructor for lifecycle-only tests. */
    AssetReturnService(AssetService assetService, AssetAssignmentService assignmentService) {
        this(assetService, assignmentService, null, null);
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

        String normalizedReturnId = ReferenceNumberNormalizer.returnId(returnId);
        String normalizedAssetId = com.recnaile.asset.model.AssetId.normalize(assetId);
        // The Return Service event and the administrative recovery endpoint can
        // race or arrive in either order. Treat returnId as the business operation
        // identity: a replay for the same asset returns its existing history, while
        // reusing it for another asset must never close a second assignment.
        if (assignmentService.hasProcessedReturnId(normalizedReturnId)) {
            return assignmentService.assignmentsForReturnId(normalizedReturnId).stream()
                    .filter(existing -> normalizedAssetId.equals(existing.getAssetId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Return ID has already been applied to a different asset: " + normalizedReturnId));
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

        AssetAssignment closed = assignmentService.closeAssignment(assetId, returnedAt, returnId, condition);
        enqueueRecoveryReturn(asset, normalizedOutwardNumber, returnId, condition, returnedAt);
        return closed;
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

        // Return Service may redeliver the same business return under a different
        // Kafka event ID. Assignment-level returnId idempotency prevents that replay
        // from closing another asset in the same outward/item group.
        if (assignmentService.hasProcessedReturnId(returnId)) {
            return;
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


    private void enqueueRecoveryReturn(Asset asset, String outwardNumber, String returnId,
            AssetCondition condition, Instant returnedAt) {
        if (outboxService == null || kafkaProperties == null) {
            // Compatibility for isolated unit tests that exercise only asset lifecycle behavior.
            return;
        }
        String normalizedReturnId = ReferenceNumberNormalizer.returnId(returnId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("returnId", normalizedReturnId);
        payload.put("outwardNumber", outwardNumber);
        payload.put("outwardId", outwardNumber);
        payload.put("itemId", asset.getItemId());
        payload.put("qty", 1);
        payload.put("condition", condition.name());
        // The Asset consumer must not apply the same return a second time after it publishes it.
        payload.put("origin", "ASSET_SERVICE_RECOVERY");
        payload.put("assetId", asset.getAssetId());
        payload.put("returnedAt", returnedAt.toString());
        outboxService.enqueueWithEventId(
                "asset-recovery-return:" + normalizedReturnId,
                kafkaProperties.topics().returnRecorded(),
                normalizedReturnId,
                "RETURN_RECORDED",
                payload);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
