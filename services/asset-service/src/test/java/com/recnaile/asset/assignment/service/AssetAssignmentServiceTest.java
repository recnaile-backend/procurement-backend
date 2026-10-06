package com.recnaile.asset.assignment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.recnaile.asset.assignment.model.AssetAssignment;
import com.recnaile.asset.assignment.repository.AssetAssignmentRepository;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.AssetRepository;

class AssetAssignmentServiceTest {

    @Test
    void assignCreatesHistoryAndUpdatesAssetSnapshot() {
        AssetRepository assets = mock(AssetRepository.class);
        AssetAssignmentRepository assignments = mock(AssetAssignmentRepository.class);
        Instant now = Instant.parse("2026-10-06T10:00:00Z");
        Asset asset = Asset.create(AssetId.of("ASSET%2F26-27%2F001"), "ITEM-001", "OUT-001",
                "Laptop", "Dell", "Latitude", "EMP-ADMIN", AssetStatus.RETURNED,
                AssetCondition.GOOD, null, now);
        when(assets.findById(asset.getAssetId())).thenReturn(Optional.of(asset));
        when(assignments.findFirstByAssetIdAndActiveTrue(asset.getAssetId())).thenReturn(Optional.empty());
        when(assignments.save(any(AssetAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assets.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetAssignmentService service = new AssetAssignmentService(assets, assignments);
        AssetAssignment result = service.assign(asset.getAssetId(), "EMP-001", "IT", "EMP-ADMIN", "Issued", now);

        assertTrue(result.isActive());
        assertEquals("EMP-001", asset.getAssignedUserId());
        assertEquals("IT", asset.getDepartmentName());
        verify(assignments).save(result);
        verify(assets).save(asset);
    }

    @Test
    void assignRejectsSecondActiveAssignment() {
        AssetRepository assets = mock(AssetRepository.class);
        AssetAssignmentRepository assignments = mock(AssetAssignmentRepository.class);
        Instant now = Instant.parse("2026-10-06T10:00:00Z");
        Asset asset = Asset.create(AssetId.of("ASSET%2F26-27%2F001"), "ITEM-001", "OUT-001",
                "Laptop", "Dell", "Latitude", "EMP-ADMIN", AssetStatus.RETURNED,
                AssetCondition.GOOD, null, now);
        when(assets.findById(asset.getAssetId())).thenReturn(Optional.of(asset));
        when(assignments.findFirstByAssetIdAndActiveTrue(asset.getAssetId()))
                .thenReturn(Optional.of(AssetAssignment.create(asset.getAssetId(), asset.getItemId(), "EMP-OLD", "IT", "EMP-ADMIN", now, null)));

        AssetAssignmentService service = new AssetAssignmentService(assets, assignments);

        assertThrows(IllegalStateException.class,
                () -> service.assign(asset.getAssetId(), "EMP-001", "IT", "EMP-ADMIN", null, now));
        verify(assignments, never()).save(any(AssetAssignment.class));
    }
}
