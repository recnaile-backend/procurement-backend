package com.recnaile.asset.event;

import java.math.BigDecimal;
import java.util.List;
import java.time.Clock;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.repository.ProcessedEventRepository;
import com.recnaile.asset.service.AssetAssignmentService;
import com.recnaile.asset.service.AssetReturnService;
import com.recnaile.asset.service.AssetService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles cross-service business events that change the reusable-asset
 * lifecycle.
 *
 * <p>Inventory consumes StockIssueApproved and owns stock deduction. Asset
 * instead consumes Outward's OUTWARD_ISSUED event because that event is the
 * authoritative notification that an approved outward was actually issued and
 * it contains the recipient/department context required for asset assignment.</p>
 */
@Service
public class AssetEventHandler {

    public static final String OUTWARD_ISSUED = "OUTWARD_ISSUED";
    public static final String RETURN_RECORDED = "RETURN_RECORDED";

    /** Legacy event names retained for parser/backward-compatible unit tests. */
    public static final String STOCK_ISSUE_APPROVED = "StockIssueApproved";
    public static final String RETURN_ACCEPTED = "ReturnAccepted";

    private final AssetService assetService;
    private final AssetAssignmentService assignmentService;
    private final AssetReturnService returnService;
    private final ProcessedEventRepository processedEventRepository;
    private final Clock clock;

    public AssetEventHandler(
            AssetService assetService,
            AssetAssignmentService assignmentService,
            AssetReturnService returnService,
            ProcessedEventRepository processedEventRepository,
            Clock clock) {
        this.assetService = assetService;
        this.assignmentService = assignmentService;
        this.returnService = returnService;
        this.processedEventRepository = processedEventRepository;
        this.clock = clock;
    }

    @Transactional
    public void handle(AssetEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Asset event ID is required");
        }
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        switch (event.eventType()) {
            case OUTWARD_ISSUED -> handleOutwardIssued(event.payload());
            case RETURN_RECORDED -> handleReturnRecorded(event.payload());
            // Kept for compatibility with already-produced legacy messages.
            case STOCK_ISSUE_APPROVED -> handleLegacyStockIssueApproved(event.payload());
            case RETURN_ACCEPTED -> handleLegacyReturnAccepted(event.payload());
            default -> throw new IllegalArgumentException("Unsupported asset event type: " + event.eventType());
        }

        processedEventRepository.save(
                ProcessedEvent.of(event.eventId(), event.eventType(), Instant.now(clock)));
    }

    private void handleOutwardIssued(JsonNode payload) {
        String outwardNumber = requiredText(payload, "outwardNumber", "outwardId", "issueReference");
        String employeeId = requiredText(payload, "recipientEmployeeId", "userId", "employeeId");
        String departmentName = requiredText(payload, "departmentName");
        String issuedBy = requiredText(payload, "issuerId", "issuedBy", "approvedBy", "requestedBy");

        JsonNode items = payload.path("items");
        if (!items.isArray()) {
            throw new IllegalArgumentException("items is required and must be an array");
        }

        for (JsonNode item : items) {
            boolean reusable = requiredBoolean(item, "assetTracked");
            if (!reusable) {
                continue;
            }

            String itemId = requiredText(item, "itemId");
            int quantity = requiredPositiveInteger(item, "quantity");
            String name = firstText(item, "name", "itemName", "assetName");
            if (name == null) {
                name = itemId;
            }
            String brand = firstText(item, "brand");
            String model = firstText(item, "model");
            String remarks = firstText(payload, "purpose", "remarks");

            List<Asset> assets = assetService.getByOutwardNumberAndItem(outwardNumber, itemId);
            for (int unit = assets.size(); unit < quantity; unit++) {
                assets = new java.util.ArrayList<>(assets);
                assets.add(assetService.create(
                        itemId,
                        outwardNumber,
                        name,
                        brand,
                        model,
                        issuedBy,
                        AssetCondition.GOOD,
                        remarks,
                        Instant.now(clock)));
            }

            if (assets.size() != quantity) {
                throw new IllegalStateException(
                        "Asset count does not match reusable issue quantity for outward " + outwardNumber
                                + " and item " + itemId);
            }

            for (Asset asset : assets) {
                if (asset.getAssignedUserId() == null) {
                    assignmentService.startAssignment(
                            asset.getAssetId(),
                            employeeId,
                            departmentName,
                            issuedBy,
                            outwardNumber,
                            Instant.now(clock),
                            remarks);
                }
            }
        }
    }

    private void handleReturnRecorded(JsonNode payload) {
        String outwardNumber = requiredText(payload, "outwardNumber");
        String itemId = requiredText(payload, "itemId");
        int quantity = requiredPositiveInteger(payload, "qty", "quantity");
        String returnId = requiredText(payload, "returnId");
        AssetCondition condition = parseCondition(requiredText(payload, "condition"));

        returnService.processReturnForOutwardItem(
                outwardNumber,
                itemId,
                quantity,
                returnId,
                condition,
                Instant.now(clock));
    }

    /**
     * Compatibility path for messages produced by older Outward versions.
     * New deployments do not subscribe the Asset Service to this topic.
     */
    private void handleLegacyStockIssueApproved(JsonNode payload) {
        boolean reusable = requiredBoolean(payload, "reusable");
        if (!reusable) {
            return;
        }

        String outwardNumber = requiredText(payload, "outwardNumber", "outwardId", "issueReference");
        String itemId = requiredText(payload, "itemId");
        String userId = requiredText(payload, "userId", "employeeId", "requestedBy");
        String departmentName = requiredText(payload, "departmentName");
        String issuedBy = requiredText(payload, "issuedBy", "approvedBy", "userId");
        String name = firstText(payload, "name", "itemName", "assetName");
        if (name == null) {
            name = itemId;
        }

        List<Asset> legacyAssets = assetService.getByOutwardNumberAndItem(outwardNumber, itemId);
        Asset asset;
        if (legacyAssets.isEmpty()) {
            asset = assetService.create(
                    itemId,
                    outwardNumber,
                    name,
                    firstText(payload, "brand"),
                    firstText(payload, "model"),
                    issuedBy,
                    AssetCondition.GOOD,
                    firstText(payload, "remarks"),
                    Instant.now(clock));
        } else {
            asset = legacyAssets.get(0);
        }

        if (asset.getAssignedUserId() == null) {
            assignmentService.startAssignment(
                    asset.getAssetId(),
                    userId,
                    departmentName,
                    issuedBy,
                    outwardNumber,
                    Instant.now(clock),
                    firstText(payload, "remarks"));
        }
    }

    /** Compatibility path for the previous ReturnAccepted contract. */
    private void handleLegacyReturnAccepted(JsonNode payload) {
        String assetId = firstText(payload, "assetId");
        String outwardNumber = firstText(payload, "outwardNumber", "outwardId", "issueReference");
        if (assetId == null) {
            outwardNumber = requiredText(payload, "outwardNumber", "outwardId", "issueReference");
            assetId = assetService.getByOutwardNumber(outwardNumber).get(0).getAssetId();
        }
        if (outwardNumber == null) {
            outwardNumber = assetService.get(assetId).getOutwardNumber();
        }
        String returnId = requiredText(payload, "returnId");
        AssetCondition condition = parseCondition(requiredText(payload, "condition"));
        returnService.processReturn(assetId, outwardNumber, returnId, condition, Instant.now(clock));
    }


    private static AssetCondition parseCondition(String value) {
        try {
            AssetCondition condition = AssetCondition.valueOf(value.trim().toUpperCase());
            if (condition != AssetCondition.GOOD && condition != AssetCondition.DAMAGED) {
                throw new IllegalArgumentException("Return condition must be GOOD or DAMAGED");
            }
            return condition;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported asset return condition: " + value, ex);
        }
    }

    private static boolean requiredBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw new IllegalArgumentException(field + " is required and must be boolean");
        }
        return value.booleanValue();
    }

    private static String requiredText(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        if (value == null) {
            throw new IllegalArgumentException(fields[0] + " is required");
        }
        return value;
    }

    private static int requiredPositiveInteger(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isNumber()) {
                try {
                    int quantity = new BigDecimal(value.asText()).intValueExact();
                    if (quantity > 0) {
                        return quantity;
                    }
                } catch (ArithmeticException ignored) {
                    // Fall through to the common validation message.
                }
            }
        }
        throw new IllegalArgumentException(fields[0] + " is required and must be a positive integer for asset-tracked items");
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode() && !value.isNull()) {
                String text = value.asText();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}
