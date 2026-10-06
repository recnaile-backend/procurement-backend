package com.recnaile.asset.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AssetLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");

    private Asset asset(AssetStatus status, AssetCondition condition) {
        return Asset.create(AssetId.of("ASSET%2F26-27%2F001"), "ITEM-001", "OUT%2F26-27%2F001",
                "Laptop", "Dell", "Latitude 5450", "EMP-ADMIN", status, condition, null, NOW);
    }

    @Test
    void assignmentMovesReturnedAssetToInUse() {
        Asset asset = asset(AssetStatus.RETURNED, AssetCondition.GOOD);
        asset.assignTo("EMP-001", "IT", NOW.plusSeconds(1));
        assertEquals(AssetStatus.IN_USE, asset.getStatus());
        assertEquals("EMP-001", asset.getAssignedUserId());
    }

    @Test
    void goodReturnMovesAssetToReturned() {
        Asset asset = asset(AssetStatus.RETURNED, AssetCondition.GOOD);
        asset.assignTo("EMP-001", "IT", NOW);
        asset.markReturned(AssetCondition.GOOD, NOW.plusSeconds(60));
        assertEquals(AssetStatus.RETURNED, asset.getStatus());
        assertEquals(AssetCondition.GOOD, asset.getCondition());
        assertNull(asset.getAssignedUserId());
    }

    @Test
    void damagedReturnMovesAssetToUnderRepair() {
        Asset asset = asset(AssetStatus.RETURNED, AssetCondition.GOOD);
        asset.assignTo("EMP-001", "IT", NOW);
        asset.markReturned(AssetCondition.DAMAGED, NOW.plusSeconds(60));
        assertEquals(AssetStatus.UNDER_REPAIR, asset.getStatus());
        assertEquals(AssetCondition.DAMAGED, asset.getCondition());
        assertNull(asset.getAssignedUserId());
    }

    @Test
    void repairCompletionMakesAssetAvailable() {
        Asset asset = asset(AssetStatus.UNDER_REPAIR, AssetCondition.DAMAGED);
        asset.completeRepair(NOW.plusSeconds(120));
        assertEquals(AssetStatus.RETURNED, asset.getStatus());
        assertEquals(AssetCondition.GOOD, asset.getCondition());
    }

    @Test
    void retiredIsTerminal() {
        Asset asset = asset(AssetStatus.RETURNED, AssetCondition.GOOD);
        asset.retire(NOW.plusSeconds(1));
        assertEquals(AssetStatus.RETIRED, asset.getStatus());
        assertThrows(IllegalStateException.class,
                () -> asset.assignTo("EMP-001", "IT", NOW.plusSeconds(2)));
    }

    @Test
    void invalidTransitionsAreRejected() {
        Asset returned = asset(AssetStatus.RETURNED, AssetCondition.GOOD);
        assertThrows(IllegalStateException.class,
                () -> returned.completeRepair(NOW.plusSeconds(1)));
        assertThrows(IllegalStateException.class,
                () -> returned.markReturned(AssetCondition.GOOD, NOW.plusSeconds(1)));

        Asset inUse = asset(AssetStatus.IN_USE, AssetCondition.GOOD);
        assertThrows(IllegalStateException.class,
                () -> inUse.retire(NOW.plusSeconds(1)));
    }
}
