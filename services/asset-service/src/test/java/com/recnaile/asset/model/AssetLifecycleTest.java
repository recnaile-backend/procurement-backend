package com.recnaile.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AssetLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-05T11:00:00Z");

    @Test
    void goodReturnMovesInUseAssetToReturnedAndClearsAssignment() {
        Asset asset = asset();
        asset.assignTo("EMP-1", "Engineering", NOW);

        asset.returnFromAssignment(AssetCondition.GOOD, LATER);

        assertEquals(AssetStatus.RETURNED, asset.getStatus());
        assertEquals(AssetCondition.GOOD, asset.getCondition());
        assertNull(asset.getAssignedUserId());
        assertNull(asset.getDepartmentName());
    }

    @Test
    void damagedReturnMovesInUseAssetToUnderRepairAndClearsAssignment() {
        Asset asset = asset();
        asset.assignTo("EMP-1", "Engineering", NOW);

        asset.returnFromAssignment(AssetCondition.DAMAGED, LATER);

        assertEquals(AssetStatus.UNDER_REPAIR, asset.getStatus());
        assertEquals(AssetCondition.DAMAGED, asset.getCondition());
        assertNull(asset.getAssignedUserId());
        assertNull(asset.getDepartmentName());
    }

    @Test
    void returnedAssetCanStartNextLifecycle() {
        Asset asset = asset();
        asset.assignTo("EMP-1", "Engineering", NOW);
        asset.returnFromAssignment(AssetCondition.GOOD, LATER);

        asset.beginNewLifecycle("EMP-2", "Finance", LATER.plusSeconds(1));

        assertEquals(AssetStatus.IN_USE, asset.getStatus());
        assertEquals("EMP-2", asset.getAssignedUserId());
        assertEquals("Finance", asset.getDepartmentName());
    }

    @Test
    void repairCompletionMakesAssetReturnedAndGood() {
        Asset asset = asset();
        asset.assignTo("EMP-1", "Engineering", NOW);
        asset.returnFromAssignment(AssetCondition.DAMAGED, LATER);

        asset.completeRepair(LATER.plusSeconds(1));

        assertEquals(AssetStatus.RETURNED, asset.getStatus());
        assertEquals(AssetCondition.GOOD, asset.getCondition());
    }

    @Test
    void retiredAssetIsTerminal() {
        Asset asset = returnedAsset();
        asset.retire(NOW);

        assertEquals(AssetStatus.RETIRED, asset.getStatus());
        assertThrows(IllegalStateException.class,
                () -> asset.assignTo("EMP-1", "Engineering", LATER));
        assertThrows(IllegalStateException.class,
                () -> asset.completeRepair(LATER));
    }

    @Test
    void underRepairAssetCannotBeAssigned() {
        Asset asset = asset();
        asset.assignTo("EMP-1", "Engineering", NOW);
        asset.returnFromAssignment(AssetCondition.DAMAGED, LATER);

        assertThrows(IllegalStateException.class,
                () -> asset.assignTo("EMP-2", "Finance", LATER.plusSeconds(1)));
    }

    @Test
    void onlyInUseAssetCanBeReturned() {
        Asset asset = asset();
        assertThrows(IllegalStateException.class,
                () -> asset.returnFromAssignment(AssetCondition.GOOD, NOW));
    }

    @Test
    void assignedAssetCannotBeRetired() {
        Asset asset = asset();
        asset.assignTo("EMP-1", "Engineering", NOW);

        assertThrows(IllegalStateException.class, () -> asset.retire(LATER));
    }

    private Asset asset() {
        return Asset.create(
                new AssetId("ASSET%2F26-27%2F001"),
                "ITEM-1",
                "OUT-1",
                "Laptop",
                "Brand",
                "Model",
                "ISSUER-1",
                AssetStatus.IN_USE,
                AssetCondition.GOOD,
                "",
                NOW);
    }

    private Asset returnedAsset() {
        return Asset.create(
                new AssetId("ASSET%2F26-27%2F001"),
                "ITEM-1",
                "OUT-1",
                "Laptop",
                "Brand",
                "Model",
                "ISSUER-1",
                AssetStatus.RETURNED,
                AssetCondition.GOOD,
                "",
                NOW);
    }
}
