package com.recnaile.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.AssetRepository;

@ExtendWith(MockitoExtension.class)
class AssetLifecycleServiceTest {

    @Mock
    private AssetRepository assetRepository;

    private AssetLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new AssetLifecycleService(assetRepository);
    }

    @Test
    void markReturnedShouldApplyGoodReturn() {
        Asset asset = assignedAsset();
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = service.markReturned(
                "asset-001", AssetCondition.GOOD, Instant.parse("2026-10-05T10:00:00Z"));

        assertEquals(AssetStatus.RETURNED, result.getStatus());
        assertEquals(AssetCondition.GOOD, result.getCondition());
    }

    @Test
    void markReturnedShouldApplyDamagedReturn() {
        Asset asset = assignedAsset();
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = service.markReturned(
                "asset-001", AssetCondition.DAMAGED, Instant.parse("2026-10-05T10:00:00Z"));

        assertEquals(AssetStatus.UNDER_REPAIR, result.getStatus());
        assertEquals(AssetCondition.DAMAGED, result.getCondition());
    }

    @Test
    void beginRepairShouldMoveReturnedDamagedAssetToUnderRepair() {
        Asset asset = assignedAsset();
        asset.returnFromAssignment(AssetCondition.DAMAGED, Instant.parse("2026-10-05T10:00:00Z"));
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = service.beginRepair("asset-001", Instant.parse("2026-10-05T10:30:00Z"));

        assertEquals(AssetStatus.UNDER_REPAIR, result.getStatus());
        assertEquals(AssetCondition.UNDER_REPAIR, result.getCondition());
    }

    @Test
    void beginRepairShouldRejectGoodReturnedAsset() {
        Asset asset = assignedAsset();
        asset.returnFromAssignment(AssetCondition.GOOD, Instant.parse("2026-10-05T10:00:00Z"));
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));

        assertThrows(IllegalStateException.class,
                () -> service.beginRepair("asset-001", Instant.parse("2026-10-05T10:30:00Z")));
    }

    @Test
    void completeRepairShouldReturnAssetToAvailableState() {
        Asset asset = assignedAsset();
        asset.returnFromAssignment(AssetCondition.DAMAGED, Instant.parse("2026-10-05T10:00:00Z"));
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = service.completeRepair("asset-001", Instant.parse("2026-10-05T11:00:00Z"));

        assertEquals(AssetStatus.RETURNED, result.getStatus());
        assertEquals(AssetCondition.GOOD, result.getCondition());
    }

    @Test
    void retireShouldRejectAssetUnderRepair() {
        Asset asset = assignedAsset();
        asset.returnFromAssignment(AssetCondition.DAMAGED, Instant.parse("2026-10-05T10:00:00Z"));
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));

        assertThrows(IllegalStateException.class,
                () -> service.retire("asset-001", Instant.parse("2026-10-05T11:00:00Z")));
    }

    @Test
    void retireShouldRejectAssignedAsset() {
        Asset asset = assignedAsset();
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));

        assertThrows(IllegalStateException.class,
                () -> service.retire("asset-001", Instant.parse("2026-10-05T10:00:00Z")));
    }

    private Asset assignedAsset() {
        Asset asset = Asset.create(
                new AssetId("asset-001"),
                "ITEM-001", "OUT/26-27/001", "Laptop", "Brand", "Model",
                "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD, null,
                Instant.parse("2026-10-05T08:00:00Z"));
        asset.assignTo("EMP-001", "Engineering", Instant.parse("2026-10-05T09:00:00Z"));
        return asset;
    }
}
