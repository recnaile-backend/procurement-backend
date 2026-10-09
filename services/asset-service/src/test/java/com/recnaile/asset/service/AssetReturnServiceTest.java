package com.recnaile.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

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
import com.recnaile.asset.outbox.AssetOutboxService;
import com.recnaile.asset.config.KafkaEventProperties;

@ExtendWith(MockitoExtension.class)
class AssetReturnServiceTest {

    @Mock AssetService assetService;
    @Mock AssetAssignmentService assignmentService;
    @Mock AssetOutboxService outboxService;
    @Mock KafkaEventProperties kafkaProperties;
    @Mock KafkaEventProperties.Topics topics;

    private AssetReturnService service;

    @BeforeEach
    void setUp() {
        service = new AssetReturnService(assetService, assignmentService);
    }


    @Test
    void administrativeReturnWritesCanonicalReturnRecordedOutboxEvent() {
        Instant returnedAt = Instant.parse("2026-10-08T12:00:00Z");
        Asset asset = assignedAsset();
        AssetAssignment assignment = AssetAssignment.start(
                "ASSET%2F26-27%2F001", "EMP-001", "Engineering", "PROC-001",
                "OUT%2F26-27%2F001", Instant.parse("2026-10-01T09:00:00Z"), null);
        when(assetService.get("ASSET%2F26-27%2F001")).thenReturn(asset);
        when(assignmentService.getActiveAssignment("ASSET%2F26-27%2F001")).thenReturn(assignment);
        when(assignmentService.closeAssignment(any(), any(), any(), any())).thenReturn(assignment);
        when(kafkaProperties.topics()).thenReturn(topics);
        when(topics.returnRecorded()).thenReturn("return.recorded");
        AssetReturnService eventPublishingService = new AssetReturnService(
                assetService, assignmentService, outboxService, kafkaProperties);

        eventPublishingService.processReturn("ASSET%2F26-27%2F001", "OUT%2F26-27%2F001",
                "RETURN%2F26-27%2F001", AssetCondition.GOOD, returnedAt);

        verify(outboxService).enqueueWithEventId(
                eq("asset-recovery-return:RETURN%2F26-27%2F001"), eq("return.recorded"),
                eq("RETURN%2F26-27%2F001"), eq("RETURN_RECORDED"), any());
    }

    @Test
    void goodReturnClosesAssignmentAndPreservesGoodCondition() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        Asset asset = assignedAsset();
        AssetAssignment assignment = AssetAssignment.start(
                "ASSET%2F26-27%2F001", "EMP-001", "Engineering", "PROC-001",
                "OUT%2F26-27%2F001", Instant.parse("2026-10-01T09:00:00Z"), null);

        when(assetService.get("ASSET%2F26-27%2F001")).thenReturn(asset);
        when(assignmentService.getActiveAssignment("ASSET%2F26-27%2F001")).thenReturn(assignment);
        when(assignmentService.closeAssignment(
                eq("ASSET%2F26-27%2F001"), eq(returnedAt), eq("RETURN%2F26-27%2F001"), eq(AssetCondition.GOOD)))
                .thenAnswer(invocation -> {
                    assignment.close(returnedAt, "RETURN%2F26-27%2F001", AssetCondition.GOOD);
                    return assignment;
                });

        AssetAssignment result = service.processReturn(
                "ASSET%2F26-27%2F001", "OUT%2F26-27%2F001", "RETURN%2F26-27%2F001", AssetCondition.GOOD, returnedAt);

        assertEquals("RETURN%2F26-27%2F001", result.getReturnId());
        verify(assignmentService).closeAssignment(
                "ASSET%2F26-27%2F001", returnedAt, "RETURN%2F26-27%2F001", AssetCondition.GOOD);
    }

    @Test
    void sameReturnIdForSameAssetIsAReplayAndDoesNotCloseAnotherAssignment() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        AssetAssignment historical = AssetAssignment.start(
                "ASSET%2F26-27%2F001", "EMP-001", "Engineering", "PROC-001",
                "OUT%2F26-27%2F001", Instant.parse("2026-10-01T09:00:00Z"), null);
        historical.close(returnedAt, "RETURN%2F26-27%2F001", AssetCondition.GOOD);
        when(assignmentService.hasProcessedReturnId("RETURN%2F26-27%2F001")).thenReturn(true);
        when(assignmentService.assignmentsForReturnId("RETURN%2F26-27%2F001"))
                .thenReturn(List.of(historical));

        AssetAssignment result = service.processReturn(
                "ASSET%2F26-27%2F001", "OUT%2F26-27%2F001", "RETURN%2F26-27%2F001",
                AssetCondition.GOOD, returnedAt);

        assertEquals(historical.getAssignmentId(), result.getAssignmentId());
        verify(assignmentService, never()).closeAssignment(any(), any(), any(), any());
        verify(assetService, never()).get(any());
    }

    @Test
    void sameReturnIdCannotReturnASecondAsset() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        AssetAssignment historical = AssetAssignment.start(
                "ASSET%2F26-27%2F001", "EMP-001", "Engineering", "PROC-001",
                "OUT%2F26-27%2F001", Instant.parse("2026-10-01T09:00:00Z"), null);
        historical.close(returnedAt, "RETURN%2F26-27%2F001", AssetCondition.GOOD);
        when(assignmentService.hasProcessedReturnId("RETURN%2F26-27%2F001")).thenReturn(true);
        when(assignmentService.assignmentsForReturnId("RETURN%2F26-27%2F001"))
                .thenReturn(List.of(historical));

        assertThrows(IllegalStateException.class, () -> service.processReturn(
                "ASSET%2F26-27%2F002", "OUT%2F26-27%2F001", "RETURN%2F26-27%2F001",
                AssetCondition.GOOD, returnedAt));

        verify(assignmentService, never()).closeAssignment(any(), any(), any(), any());
        verify(assetService, never()).get(any());
    }

    @Test
    void damagedReturnClosesAssignmentWithDamagedCondition() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        Asset asset = assignedAsset();
        AssetAssignment assignment = AssetAssignment.start(
                "ASSET%2F26-27%2F001", "EMP-001", "Engineering", "PROC-001",
                "OUT%2F26-27%2F001", Instant.parse("2026-10-01T09:00:00Z"), null);

        when(assetService.get("ASSET%2F26-27%2F001")).thenReturn(asset);
        when(assignmentService.getActiveAssignment("ASSET%2F26-27%2F001")).thenReturn(assignment);
        when(assignmentService.closeAssignment(
                eq("ASSET%2F26-27%2F001"), eq(returnedAt), eq("RETURN%2F26-27%2F002"), eq(AssetCondition.DAMAGED)))
                .thenReturn(assignment);

        service.processReturn(
                "ASSET%2F26-27%2F001", "OUT%2F26-27%2F001", "RETURN%2F26-27%2F002", AssetCondition.DAMAGED, returnedAt);

        verify(assignmentService).closeAssignment(
                "ASSET%2F26-27%2F001", returnedAt, "RETURN%2F26-27%2F002", AssetCondition.DAMAGED);
    }


    @Test
    void returnRecordedClosesExactlyTheRequestedReusableUnits() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        Asset first = assignedAsset();
        Asset second = Asset.create(
                new AssetId("ASSET%2F26-27%2F002"), "ITEM-001", "OUT%2F26-27%2F001", "Laptop",
                "Brand", "Model", "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD, null,
                Instant.parse("2026-10-01T08:00:00Z"));
        second.assignTo("EMP-001", "Engineering", Instant.parse("2026-10-01T09:00:00Z"));
        AssetAssignment firstAssignment = AssetAssignment.start(
                first.getAssetId(), "EMP-001", "Engineering", "PROC-001", "OUT%2F26-27%2F001",
                Instant.parse("2026-10-01T09:00:00Z"), null);
        AssetAssignment secondAssignment = AssetAssignment.start(
                second.getAssetId(), "EMP-001", "Engineering", "PROC-001", "OUT%2F26-27%2F001",
                Instant.parse("2026-10-01T09:01:00Z"), null);

        when(assignmentService.activeAssignmentsForOutward("OUT%2F26-27%2F001"))
                .thenReturn(List.of(firstAssignment, secondAssignment));
        when(assetService.get(first.getAssetId())).thenReturn(first);
        when(assetService.get(second.getAssetId())).thenReturn(second);
        when(assignmentService.closeAssignment(any(), eq(returnedAt), eq("RETURN%2F26-27%2F001"), eq(AssetCondition.GOOD)))
                .thenReturn(firstAssignment);

        service.processReturnForOutwardItem(
                "OUT%2F26-27%2F001", "ITEM-001", 2, "RETURN%2F26-27%2F001", AssetCondition.GOOD, returnedAt);

        verify(assignmentService).closeAssignment(
                first.getAssetId(), returnedAt, "RETURN%2F26-27%2F001", AssetCondition.GOOD);
        verify(assignmentService).closeAssignment(
                second.getAssetId(), returnedAt, "RETURN%2F26-27%2F001", AssetCondition.GOOD);
    }

    @Test
    void duplicateReturnIdDoesNotCloseAnotherAssetEvenWhenEventIdChanges() {
        when(assignmentService.hasProcessedReturnId("RETURN%2F26-27%2F001")).thenReturn(true);

        service.processReturnForOutwardItem(
                "OUT%2F26-27%2F001", "ITEM-001", 1, "RETURN%2F26-27%2F001",
                AssetCondition.GOOD, Instant.parse("2026-10-06T09:00:00Z"));

        verify(assignmentService).hasProcessedReturnId("RETURN%2F26-27%2F001");
        verify(assignmentService, never()).activeAssignmentsForOutward(any());
        verify(assignmentService, never()).closeAssignment(any(), any(), any(), any());
    }

    @Test
    void returnRecordedRejectsQuantityGreaterThanActiveReusableUnits() {
        Instant returnedAt = Instant.parse("2026-10-06T09:00:00Z");
        Asset first = assignedAsset();
        AssetAssignment firstAssignment = AssetAssignment.start(
                first.getAssetId(), "EMP-001", "Engineering", "PROC-001", "OUT%2F26-27%2F001",
                Instant.parse("2026-10-01T09:00:00Z"), null);
        when(assignmentService.activeAssignmentsForOutward("OUT%2F26-27%2F001"))
                .thenReturn(List.of(firstAssignment));
        when(assetService.get(first.getAssetId())).thenReturn(first);

        assertThrows(IllegalStateException.class, () -> service.processReturnForOutwardItem(
                "OUT%2F26-27%2F001", "ITEM-001", 2, "RETURN%2F26-27%2F002", AssetCondition.GOOD, returnedAt));
        verify(assignmentService, never()).closeAssignment(any(), any(), any(), any());
    }

    @Test
    void returnRejectsNonReturnCondition() {
        assertThrows(IllegalArgumentException.class, () -> service.processReturn(
                "ASSET%2F26-27%2F001", "OUT%2F26-27%2F001", "RETURN%2F26-27%2F003", AssetCondition.UNDER_REPAIR,
                Instant.parse("2026-10-06T09:00:00Z")));
    }

    @Test
    void returnRejectsAssetThatIsNotCurrentlyAssigned() {
        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"), "ITEM-001", "OUT%2F26-27%2F001", "Laptop",
                "Brand", "Model", "PROC-001", AssetStatus.RETURNED, AssetCondition.GOOD,
                null, Instant.parse("2026-10-01T08:00:00Z"));
        when(assetService.get("ASSET%2F26-27%2F001")).thenReturn(asset);

        assertThrows(IllegalStateException.class, () -> service.processReturn(
                "ASSET%2F26-27%2F001", "OUT%2F26-27%2F001", "RETURN%2F26-27%2F004", AssetCondition.GOOD,
                Instant.parse("2026-10-06T09:00:00Z")));
    }

    private Asset assignedAsset() {
        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"), "ITEM-001", "OUT%2F26-27%2F001", "Laptop",
                "Brand", "Model", "PROC-001", AssetStatus.IN_USE, AssetCondition.GOOD,
                null, Instant.parse("2026-10-01T08:00:00Z"));
        asset.assignTo("EMP-001", "Engineering", Instant.parse("2026-10-01T09:00:00Z"));
        return asset;
    }
}
