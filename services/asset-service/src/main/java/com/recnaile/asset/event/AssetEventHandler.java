package com.recnaile.asset.event;

import java.time.Clock;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.exception.ResourceNotFoundException;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.service.AssetAssignmentService;
import com.recnaile.asset.service.AssetLifecycleService;
import com.recnaile.asset.service.AssetService;
import com.recnaile.asset.repository.ProcessedEventRepository;

import org.springframework.stereotype.Service;

@Service
public class AssetEventHandler {

    public static final String STOCK_ISSUE_APPROVED = "StockIssueApproved";
    public static final String RETURN_ACCEPTED = "ReturnAccepted";

    private final AssetService assetService;
    private final AssetAssignmentService assignmentService;
    private final AssetLifecycleService lifecycleService;
    private final ProcessedEventRepository processedEventRepository;
    private final Clock clock;

    public AssetEventHandler(
            AssetService assetService,
            AssetAssignmentService assignmentService,
            AssetLifecycleService lifecycleService,
            ProcessedEventRepository processedEventRepository,
            Clock clock) {
        this.assetService = assetService;
        this.assignmentService = assignmentService;
        this.lifecycleService = lifecycleService;
        this.processedEventRepository = processedEventRepository;
        this.clock = clock;
    }

    public void handle(AssetEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        switch (event.eventType()) {
            case STOCK_ISSUE_APPROVED -> handleStockIssueApproved(event.payload());
            case RETURN_ACCEPTED -> handleReturnAccepted(event.payload());
            default -> throw new IllegalArgumentException("Unsupported asset event type: " + event.eventType());
        }

        processedEventRepository.save(
                ProcessedEvent.of(event.eventId(), event.eventType(), Instant.now(clock)));
    }

    private void handleStockIssueApproved(JsonNode payload) {
        boolean reusable = requiredBoolean(payload, "reusable");
        if (!reusable) {
            return;
        }

        String outwardId = requiredText(payload, "outwardId", "issueReference");
        String itemId = requiredText(payload, "itemId");
        String userId = requiredText(payload, "userId", "employeeId", "requestedBy");
        String departmentName = requiredText(payload, "departmentName");
        String issuedBy = requiredText(payload, "issuedBy", "approvedBy", "userId");
        String name = firstText(payload, "name", "itemName", "assetName");
        if (name == null) {
            name = itemId;
        }

        String brand = firstText(payload, "brand");
        String model = firstText(payload, "model");
        String remarks = firstText(payload, "remarks");

        Asset asset;
        try {
            asset = assetService.getByIssueReference(outwardId);
        } catch (ResourceNotFoundException notFound) {
            asset = assetService.create(
                    itemId,
                    outwardId,
                    name,
                    brand,
                    model,
                    issuedBy,
                    AssetCondition.GOOD,
                    remarks,
                    Instant.now(clock));
        }

        assignmentService.startAssignment(
                asset.getAssetId(),
                userId,
                departmentName,
                issuedBy,
                outwardId,
                Instant.now(clock),
                remarks);
    }

    private void handleReturnAccepted(JsonNode payload) {
        String assetId = firstText(payload, "assetId");
        if (assetId == null) {
            String issueReference = requiredText(payload, "issueReference", "outwardId");
            assetId = assetService.getByIssueReference(issueReference).getAssetId();
        }
        String returnId = requiredText(payload, "returnId");
        AssetCondition condition = parseCondition(requiredText(payload, "condition"));

        if (condition == AssetCondition.DAMAGED) {
            Asset asset = assetService.get(assetId);
            if (asset.getStatus() == com.recnaile.asset.model.AssetStatus.UNDER_REPAIR) {
                return;
            }
        }

        assignmentService.closeAssignment(
                assetId,
                Instant.now(clock),
                returnId,
                condition);
    }

    private static AssetCondition parseCondition(String value) {
        try {
            return AssetCondition.valueOf(value.trim().toUpperCase());
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
