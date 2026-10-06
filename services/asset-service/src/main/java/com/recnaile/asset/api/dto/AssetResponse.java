package com.recnaile.asset.api.dto;

import java.time.Instant;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetStatus;

public record AssetResponse(
        String assetId, String assetReference, String itemId, String issueReference,
        String name, String brand, String model, String departmentName,
        String assignedUserId, String issuedBy, AssetStatus status,
        AssetCondition condition, String remarks, Instant createdAt, Instant updatedAt, Long version) {

    public static AssetResponse from(Asset a) {
        return new AssetResponse(a.getAssetId(), a.getAssetReference(), a.getItemId(), a.getIssueReference(),
                a.getName(), a.getBrand(), a.getModel(), a.getDepartmentName(), a.getAssignedUserId(),
                a.getIssuedBy(), a.getStatus(), a.getCondition(), a.getRemarks(), a.getCreatedAt(), a.getUpdatedAt(), a.getVersion());
    }
}
