package com.recnaile.asset.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AssetTest {

    @Test
    void createsAggregateWithRequiredDomainFields() {
        Instant now = Instant.parse("2026-10-06T06:00:00Z");
        Asset asset = Asset.create(
                AssetId.of("ASSET%2F26-27%2F001"),
                "ITEM-001",
                "OUT%2F26-27%2F001",
                "Laptop",
                "Dell",
                "Latitude 5450",
                "EMP-001",
                AssetStatus.IN_USE,
                AssetCondition.GOOD,
                "Initial reusable asset",
                now);

        assertEquals("ASSET%2F26-27%2F001", asset.getAssetId());
        assertEquals("ITEM-001", asset.getItemId());
        assertEquals("OUT%2F26-27%2F001", asset.getIssueReference());
        assertEquals("Laptop", asset.getName());
        assertEquals("Dell", asset.getBrand());
        assertEquals("Latitude 5450", asset.getModel());
        assertEquals("EMP-001", asset.getIssuedBy());
        assertEquals(AssetStatus.IN_USE, asset.getStatus());
        assertEquals(AssetCondition.GOOD, asset.getCondition());
        assertEquals(now, asset.getCreatedAt());
        assertEquals(now, asset.getUpdatedAt());
        assertNull(asset.getDepartmentName());
        assertNull(asset.getAssignedUserId());
    }

    @Test
    void rejectsMissingRequiredFields() {
        Instant now = Instant.now();
        AssetId id = AssetId.of("ASSET%2F26-27%2F001");

        assertThrows(IllegalArgumentException.class,
                () -> Asset.create(id, "", "OUT-001", "Laptop", "Dell", "5450",
                        "EMP-001", AssetStatus.IN_USE, AssetCondition.GOOD, null, now));
        assertThrows(IllegalArgumentException.class,
                () -> Asset.create(id, "ITEM-001", "", "Laptop", "Dell", "5450",
                        "EMP-001", AssetStatus.IN_USE, AssetCondition.GOOD, null, now));
        assertThrows(IllegalArgumentException.class,
                () -> Asset.create(id, "ITEM-001", "OUT-001", "", "Dell", "5450",
                        "EMP-001", AssetStatus.IN_USE, AssetCondition.GOOD, null, now));
    }
}
