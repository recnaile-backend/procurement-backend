package com.recnaile.asset.lifecycle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.recnaile.asset.assignment.model.AssetAssignment;
import com.recnaile.asset.assignment.repository.AssetAssignmentRepository;
import com.recnaile.asset.lifecycle.service.AssetLifecycleService;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.AssetRepository;

class AssetLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");

    @Test
    void goodReturnClosesAssignmentAndReturnsAsset() {
        AssetRepository assets = mock(AssetRepository.class);
        AssetAssignmentRepository assignments = mock(AssetAssignmentRepository.class);
        Asset asset = Asset.create(AssetId.of("ASSET%2F26-27%2F001"), "ITEM-001", "OUT-001",
                "Laptop", "Dell", "Latitude", "EMP-ADMIN", AssetStatus.RETURNED,
                AssetCondition.GOOD, null, NOW);
        asset.assignTo("EMP-001", "IT", NOW.plusSeconds(1));
        AssetAssignment assignment = AssetAssignment.create(asset.getAssetId(), asset.getItemId(),
                "EMP-001", "IT", "EMP-ADMIN", NOW.plusSeconds(1), null);
        when(assets.findById(asset.getAssetId())).thenReturn(Optional.of(asset));
        when(assignments.findFirstByAssetIdAndActiveTrue(asset.getAssetId())).thenReturn(Optional.of(assignment));
        when(assets.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assignments.save(any(AssetAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetLifecycleService service = new AssetLifecycleService(assets, assignments);
        Asset result = service.returnAsset(asset.getAssetId(), AssetCondition.GOOD, NOW.plusSeconds(60));

        assertEquals(AssetStatus.RETURNED, result.getStatus());
        assertFalse(assignment.isActive());
        assertEquals(NOW.plusSeconds(60), assignment.getReturnedAt());
        verify(assets).save(asset);
        verify(assignments).save(assignment);
    }

    @Test
    void damagedReturnMovesAssetUnderRepair() {
        AssetRepository assets = mock(AssetRepository.class);
        AssetAssignmentRepository assignments = mock(AssetAssignmentRepository.class);
        Asset asset = Asset.create(AssetId.of("ASSET%2F26-27%2F002"), "ITEM-002", "OUT-002",
                "Monitor", "Dell", "P2422H", "EMP-ADMIN", AssetStatus.RETURNED,
                AssetCondition.GOOD, null, NOW);
        asset.assignTo("EMP-002", "IT", NOW.plusSeconds(1));
        AssetAssignment assignment = AssetAssignment.create(asset.getAssetId(), asset.getItemId(),
                "EMP-002", "IT", "EMP-ADMIN", NOW.plusSeconds(1), null);
        when(assets.findById(asset.getAssetId())).thenReturn(Optional.of(asset));
        when(assignments.findFirstByAssetIdAndActiveTrue(asset.getAssetId())).thenReturn(Optional.of(assignment));
        when(assets.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assignments.save(any(AssetAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetLifecycleService service = new AssetLifecycleService(assets, assignments);
        Asset result = service.returnAsset(asset.getAssetId(), AssetCondition.DAMAGED, NOW.plusSeconds(60));

        assertEquals(AssetStatus.UNDER_REPAIR, result.getStatus());
        assertEquals(AssetCondition.DAMAGED, result.getCondition());
        assertFalse(assignment.isActive());
    }

    @Test
    void repairCompletionReturnsAssetToAvailableState() {
        AssetRepository assets = mock(AssetRepository.class);
        AssetAssignmentRepository assignments = mock(AssetAssignmentRepository.class);
        Asset asset = Asset.create(AssetId.of("ASSET%2F26-27%2F003"), "ITEM-003", "OUT-003",
                "Phone", "Samsung", "S24", "EMP-ADMIN", AssetStatus.UNDER_REPAIR,
                AssetCondition.DAMAGED, null, NOW);
        when(assets.findById(asset.getAssetId())).thenReturn(Optional.of(asset));
        when(assets.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetLifecycleService service = new AssetLifecycleService(assets, assignments);
        Asset result = service.completeRepair(asset.getAssetId(), NOW.plusSeconds(60));

        assertEquals(AssetStatus.RETURNED, result.getStatus());
        assertEquals(AssetCondition.GOOD, result.getCondition());
        verify(assets).save(asset);
    }

    @Test
    void retirementRejectsAssetStillInUse() {
        AssetRepository assets = mock(AssetRepository.class);
        AssetAssignmentRepository assignments = mock(AssetAssignmentRepository.class);
        Asset asset = Asset.create(AssetId.of("ASSET%2F26-27%2F004"), "ITEM-004", "OUT-004",
                "Tablet", "Samsung", "Tab", "EMP-ADMIN", AssetStatus.RETURNED,
                AssetCondition.GOOD, null, NOW);
        asset.assignTo("EMP-004", "IT", NOW);
        when(assets.findById(asset.getAssetId())).thenReturn(Optional.of(asset));

        AssetLifecycleService service = new AssetLifecycleService(assets, assignments);

        assertThrows(IllegalStateException.class,
                () -> service.retire(asset.getAssetId(), NOW.plusSeconds(1)));
        verify(assets, never()).save(any(Asset.class));
    }
}
