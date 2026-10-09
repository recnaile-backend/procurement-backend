package com.recnaile.asset.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.outbox.AssetOutboxService;
import com.recnaile.asset.config.KafkaEventProperties;

/** Creates durable outbound business events in the Asset transactional outbox. */
@Service
public class AssetEventPublisher {
    public static final String ASSET_ASSIGNED = "AssetAssigned";
    public static final String ASSET_RETURNED = "AssetReturned";
    public static final String ASSET_STATUS_CHANGED = "AssetStatusChanged";

    private final AssetOutboxService outboxService;
    private final KafkaEventProperties properties;

    public AssetEventPublisher(AssetOutboxService outboxService, KafkaEventProperties properties) {
        this.outboxService = outboxService;
        this.properties = properties;
    }

    public void publishAssetAssigned(Asset asset, AssetAssignment assignment) {
        outboxService.enqueue(properties.topics().assetAssigned(), asset.getAssetId(), ASSET_ASSIGNED,
                assignmentPayload(asset, assignment));
    }

    public void publishAssetReturned(Asset asset, AssetAssignment assignment) {
        outboxService.enqueue(properties.topics().assetReturned(), asset.getAssetId(), ASSET_RETURNED,
                assignmentPayload(asset, assignment));
    }

    public void publishStatusChanged(Asset asset, String previousStatus, String currentStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assetId", asset.getAssetId());
        payload.put("itemId", asset.getItemId());
        payload.put("outwardNumber", asset.getOutwardNumber());
        payload.put("name", asset.getName());
        payload.put("previousStatus", previousStatus);
        payload.put("currentStatus", currentStatus);
        payload.put("condition", asset.getCondition() == null ? null : asset.getCondition().name());
        payload.put("assignedUserId", asset.getAssignedUserId());
        payload.put("departmentName", asset.getDepartmentName());
        payload.put("updatedAt", asset.getUpdatedAt());
        outboxService.enqueue(properties.topics().assetStatusChanged(), asset.getAssetId(), ASSET_STATUS_CHANGED, payload);
    }

    private Map<String, Object> assignmentPayload(Asset asset, AssetAssignment assignment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assetId", asset.getAssetId());
        payload.put("itemId", asset.getItemId());
        payload.put("outwardNumber", asset.getOutwardNumber());
        payload.put("name", asset.getName());
        payload.put("brand", asset.getBrand());
        payload.put("model", asset.getModel());
        payload.put("status", asset.getStatus() == null ? null : asset.getStatus().name());
        payload.put("condition", asset.getCondition() == null ? null : asset.getCondition().name());
        payload.put("assignedUserId", asset.getAssignedUserId());
        payload.put("departmentName", asset.getDepartmentName());
        payload.put("issuedBy", asset.getIssuedBy());
        payload.put("assignmentId", assignment.getAssignmentId());
        payload.put("employeeId", assignment.getEmployeeId());
        payload.put("outwardNumber", assignment.getOutwardNumber());
        payload.put("assignedAt", assignment.getAssignedAt());
        payload.put("returnedAt", assignment.getReturnedAt());
        payload.put("returnId", assignment.getReturnId());
        payload.put("returnCondition", assignment.getReturnCondition() == null ? null : assignment.getReturnCondition().name());
        payload.put("remarks", assignment.getRemarks());
        payload.put("publishedAt", Instant.now().toString());
        return payload;
    }
}
