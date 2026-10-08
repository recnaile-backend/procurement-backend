package com.recnaile.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AssetTest {

    @Test
    void createShouldBuildReusableAssetWithStableIdentityAndLifecycleFields() {
        Instant now = Instant.parse("2026-10-05T08:00:00Z");

        Asset asset = Asset.create(
                new AssetId("asset-001"), "ITEM-001", "OUT/26-27/001",
                "Laptop", "ExampleBrand", "Model-X", "PROC-001",
                AssetStatus.IN_USE, AssetCondition.GOOD, "Issued for project work", now);

        assertEquals("asset-001", asset.getAssetId());
        assertEquals("ITEM-001", asset.getItemId());
        assertEquals("OUT/26-27/001", asset.getOutwardNumber());
        assertEquals(AssetStatus.IN_USE, asset.getStatus());
        assertEquals(AssetCondition.GOOD, asset.getCondition());
        assertNull(asset.getDepartmentName());
        assertNull(asset.getAssignedUserId());
        assertEquals(now, asset.getCreatedAt());
        assertEquals(now, asset.getUpdatedAt());
    }

    @Test
    void assignToShouldSetEmployeeAndDepartmentName() {
        Asset asset = Asset.create(
                new AssetId("asset-001"), "ITEM-001", "OUT/26-27/001",
                "Laptop", null, null, "PROC-001", AssetStatus.IN_USE,
                AssetCondition.GOOD, null, Instant.parse("2026-10-05T08:00:00Z"));

        Instant assignedAt = Instant.parse("2026-10-05T09:00:00Z");
        asset.assignTo("EMP-001", "Engineering", assignedAt);

        assertEquals("EMP-001", asset.getAssignedUserId());
        assertEquals("Engineering", asset.getDepartmentName());
        assertEquals(assignedAt, asset.getUpdatedAt());
    }

    @Test
    void clearAssignmentShouldRemoveCurrentAssignmentSnapshot() {
        Asset asset = Asset.create(
                new AssetId("asset-001"), "ITEM-001", "OUT/26-27/001",
                "Laptop", null, null, "PROC-001", AssetStatus.IN_USE,
                AssetCondition.GOOD, null, Instant.parse("2026-10-05T08:00:00Z"));
        asset.assignTo("EMP-001", "Engineering", Instant.parse("2026-10-05T09:00:00Z"));

        Instant returnedAt = Instant.parse("2026-10-10T09:00:00Z");
        asset.clearAssignment(returnedAt);

        assertNull(asset.getAssignedUserId());
        assertNull(asset.getDepartmentName());
        assertEquals(returnedAt, asset.getUpdatedAt());
    }

    @Test
    void assignToShouldRejectSecondActiveAssignment() {
        Asset asset = Asset.create(
                new AssetId("asset-001"), "ITEM-001", "OUT/26-27/001",
                "Laptop", null, null, "PROC-001", AssetStatus.IN_USE,
                AssetCondition.GOOD, null, Instant.parse("2026-10-05T08:00:00Z"));
        asset.assignTo("EMP-001", "Engineering", Instant.parse("2026-10-05T09:00:00Z"));

        assertThrows(IllegalStateException.class, () -> asset.assignTo(
                "EMP-002", "Finance", Instant.parse("2026-10-06T09:00:00Z")));
    }

    @Test
    void assignToShouldRejectRetiredAsset() {
        Asset asset = Asset.create(
                new AssetId("asset-001"), "ITEM-001", "OUT/26-27/001",
                "Laptop", null, null, "PROC-001", AssetStatus.RETIRED,
                AssetCondition.DAMAGED, null, Instant.parse("2026-10-05T08:00:00Z"));

        assertThrows(IllegalStateException.class, () -> asset.assignTo(
                "EMP-001", "Engineering", Instant.parse("2026-10-05T09:00:00Z")));
    }
}
