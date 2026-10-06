package com.recnaile.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;

@ExtendWith(MockitoExtension.class)
class AssetReturnServiceTest {

    @Mock AssetService assetService;
    @Mock AssetAssignmentService assignmentService;

    private AssetReturnService service;

    @BeforeEach
    void setUp() {
        service = new AssetReturnService(assetService, assignmentService);
    }

    @Test
    void goodReturnClosesAssignmentAndPreservesGoodCondition() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        Asset asset = assignedAsset();
        AssetAssignment assignment = AssetAssignment.start(
                "ASSET%2F26-27%2F001", "EMP-001", "Engineering", "PROC-001",
                "OUT-001", Instant.parse("2026-10-01T09:00:00Z"), null);

        when(assetService.get("ASSET%2F26-27%2F001")).thenReturn(asset);
        when(assignmentService.closeAssignment(
                eq("ASSET%2F26-27%2F001"), eq(returnedAt), eq("RET-001"), eq(AssetCondition.GOOD)))
                .thenAnswer(invocation -> {
                    assignment.close(returnedAt, "RET-001", AssetCondition.GOOD);
                    return assignment;
                });

        AssetAssignment result = service.processReturn(
                "ASSET%2F26-27%2F001", "RET-001", AssetCondition.GOOD, returnedAt);

        assertEquals("RET-001", result.getReturnId());
        verify(assignmentService).closeAssignment(
                "ASSET%2F26-27%2F001", returnedAt, "RET-001", AssetCondition.GOOD);
    }

    @Test
    void damagedReturnClosesAssignmentWithDamagedCondition() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        Asset asset = assignedAsset();
        AssetAssignment assignment = AssetAssignment.start(
                "ASSET%2F26-27%2F001", "EMP-001", "Engineering", "PROC-001",
                "OUT-001", Instant.parse("2026-10-01T09:00:00Z"), null);

        when(assetService.get("ASSET%2F26-27%2F001")).thenReturn(asset);
        when(assignmentService.closeAssignment(
                eq("ASSET%2F26-27%2F001"), eq(returnedAt), eq("RET-002"), eq(AssetCondition.DAMAGED)))
                .thenReturn(assignment);

        service.processReturn(
                "ASSET%2F26-27%2F001", "RET-002", AssetCondition.DAMAGED, returnedAt);

        verify(assignmentService).closeAssignment(
                "ASSET%2F26-27%2F001", returnedAt, "RET-002", AssetCondition.DAMAGED);
    }

    @Test
    void returnRejectsNonReturnCondition() {
        assertThrows(IllegalArgumentException.class, () -> service.processReturn(
                "ASSET%2F26-27%2F001", "RET-003", AssetCondition.UNDER_REPAIR,
                Instant.parse("2026-10-06T09:00:00Z")));
    }

    @Test
    void returnRejectsAssetThatIsNotCurrentlyAssigned() {
        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"), "ITEM-001", "OUT-001", "Laptop",
                "Brand", "Model", "PROC-001", AssetStatus.RETURNED, AssetCondition.GOOD,
                null, Instant.parse("2026-10-01T08:00:00Z"));
        when(assetService.get("ASSET%2F26-27%2F001")).thenReturn(asset);

        assertThrows(IllegalStateException.class, () -> service.processReturn(
                "ASSET%2F26-27%2F001", "RET-004", AssetCondition.GOOD,
                Instant.parse("2026-10-06T09:00:00Z")));
    }

    private Asset assignedAsset() {
        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"), "ITEM-001", "OUT-001", "Laptop",
                "Brand", "Model", "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD,
                null, Instant.parse("2026-10-01T08:00:00Z"));
        asset.assignTo("EMP-001", "Engineering", Instant.parse("2026-10-01T09:00:00Z"));
        return asset;
    }
}
