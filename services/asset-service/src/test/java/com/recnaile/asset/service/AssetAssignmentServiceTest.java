package com.recnaile.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.AssetAssignmentRepository;
import com.recnaile.asset.repository.AssetRepository;

@ExtendWith(MockitoExtension.class)
class AssetAssignmentServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetAssignmentRepository assignmentRepository;

    private AssetAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssetAssignmentService(assetRepository, assignmentRepository);
    }

    @Test
    void startAssignmentShouldCreateHistoryAndUpdateCurrentAssignment() {
        Instant createdAt = Instant.parse("2026-10-05T08:00:00Z");
        Instant assignedAt = Instant.parse("2026-10-05T09:00:00Z");
        Asset asset = Asset.create(
                new com.recnaile.asset.model.AssetId("asset-001"),
                "ITEM-001", "OUT/26-27/001", "Laptop", "Brand", "Model",
                "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD, null, createdAt);

        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assignmentRepository.existsByAssetIdAndReturnedAtIsNull("asset-001")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentRepository.save(any(AssetAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetAssignment result = service.startAssignment(
                "asset-001", "EMP-001", "Engineering", "PROC-001",
                "OUT/26-27/001", assignedAt, "Laptop issued");

        assertEquals("asset-001", result.getAssetId());
        assertEquals("EMP-001", result.getEmployeeId());
        assertEquals("Engineering", result.getDepartmentName());
        assertEquals("EMP-001", asset.getAssignedUserId());
        assertEquals("Engineering", asset.getDepartmentName());
        verify(assetRepository).save(asset);
        verify(assignmentRepository).save(result);
    }

    @Test
    void startAssignmentShouldRejectWhenAnotherAssignmentIsActive() {
        Asset asset = Asset.create(
                new com.recnaile.asset.model.AssetId("asset-001"),
                "ITEM-001", "OUT/26-27/001", "Laptop", null, null,
                "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD, null,
                Instant.parse("2026-10-05T08:00:00Z"));
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assignmentRepository.existsByAssetIdAndReturnedAtIsNull("asset-001")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.startAssignment(
                "asset-001", "EMP-001", "Engineering", "PROC-001",
                "OUT/26-27/001", Instant.parse("2026-10-05T09:00:00Z"), null));

        verify(assetRepository, never()).save(any());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void closeAssignmentShouldCloseHistoryAndClearCurrentSnapshot() {
        Asset asset = Asset.create(
                new com.recnaile.asset.model.AssetId("asset-001"),
                "ITEM-001", "OUT/26-27/001", "Laptop", null, null,
                "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD, null,
                Instant.parse("2026-10-05T08:00:00Z"));
        Instant assignedAt = Instant.parse("2026-10-05T09:00:00Z");
        Instant returnedAt = Instant.parse("2026-10-10T09:00:00Z");
        asset.assignTo("EMP-001", "Engineering", assignedAt);

        AssetAssignment assignment = AssetAssignment.start(
                "asset-001", "EMP-001", "Engineering", "PROC-001",
                "OUT/26-27/001", assignedAt, null);
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assignmentRepository.findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc("asset-001"))
                .thenReturn(Optional.of(assignment));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentRepository.save(any(AssetAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetAssignment result = service.closeAssignment(
                "asset-001", returnedAt, "RET/26-27/001", AssetCondition.GOOD);

        assertEquals("RET/26-27/001", result.getReturnId());
        assertEquals(AssetCondition.GOOD, result.getReturnCondition());
        assertEquals(null, asset.getAssignedUserId());
        assertEquals(null, asset.getDepartmentName());
        verify(assetRepository).save(asset);
        verify(assignmentRepository).save(result);
    }

    @Test
    void closeAssignmentShouldRejectWhenNoActiveAssignmentExists() {
        Asset asset = Asset.create(
                new com.recnaile.asset.model.AssetId("asset-001"),
                "ITEM-001", "OUT/26-27/001", "Laptop", null, null,
                "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD, null,
                Instant.parse("2026-10-05T08:00:00Z"));
        when(assetRepository.findById("asset-001")).thenReturn(Optional.of(asset));
        when(assignmentRepository.findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc("asset-001"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.closeAssignment(
                "asset-001", Instant.parse("2026-10-10T09:00:00Z"),
                "RET/26-27/001", AssetCondition.GOOD));
    }
}
