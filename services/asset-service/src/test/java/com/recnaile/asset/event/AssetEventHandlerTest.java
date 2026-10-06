package com.recnaile.asset.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.exception.ResourceNotFoundException;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.ProcessedEventRepository;
import com.recnaile.asset.service.AssetAssignmentService;
import com.recnaile.asset.service.AssetLifecycleService;
import com.recnaile.asset.service.AssetService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetEventHandlerTest {

    private AssetService assetService;
    private AssetAssignmentService assignmentService;
    private AssetLifecycleService lifecycleService;
    private ProcessedEventRepository processedEventRepository;
    private AssetEventHandler handler;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        assetService = mock(AssetService.class);
        assignmentService = mock(AssetAssignmentService.class);
        lifecycleService = mock(AssetLifecycleService.class);
        processedEventRepository = mock(ProcessedEventRepository.class);

        handler = new AssetEventHandler(
                assetService,
                assignmentService,
                lifecycleService,
                processedEventRepository,
                Clock.fixed(Instant.parse("2026-10-06T08:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void stockIssueApprovedCreatesAndAssignsReusableAsset() throws Exception {
        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"),
                "ITEM-001", "OUT-001", "Laptop", "Brand", "Model",
                "PROC-001", AssetStatus.RETURNED, AssetCondition.GOOD, null,
                Instant.parse("2026-10-06T08:00:00Z"));

        when(processedEventRepository.existsById("evt-001")).thenReturn(false);
        when(assetService.getByIssueReference("OUT-001")).thenThrow(new ResourceNotFoundException("not found"));
        when(assetService.create(eq("ITEM-001"), eq("OUT-001"), eq("Laptop"), eq("Brand"),
                eq("Model"), eq("PROC-001"), eq(AssetCondition.GOOD), isNull(), any(Instant.class)))
                .thenReturn(asset);

        handler.handle(new AssetEvent(
                "evt-001",
                AssetEventHandler.STOCK_ISSUE_APPROVED,
                mapper.readTree("""
                        {
                          "outwardId":"OUT-001",
                          "itemId":"ITEM-001",
                          "userId":"EMP-001",
                          "departmentName":"Engineering",
                          "issuedBy":"PROC-001",
                          "name":"Laptop",
                          "brand":"Brand",
                          "model":"Model",
                          "reusable":true
                        }
                        """)));

        verify(assetService).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(assignmentService).startAssignment(
                eq("ASSET%2F26-27%2F001"), eq("EMP-001"), eq("Engineering"),
                eq("PROC-001"), eq("OUT-001"), any(Instant.class), isNull());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void nonReusableIssueIsIgnoredAndMarkedProcessed() throws Exception {
        when(processedEventRepository.existsById("evt-002")).thenReturn(false);

        handler.handle(new AssetEvent(
                "evt-002",
                AssetEventHandler.STOCK_ISSUE_APPROVED,
                mapper.readTree("""
                        {"outwardId":"OUT-002","itemId":"ITEM-002","reusable":false}
                        """)));

        verifyNoInteractions(assetService, assignmentService, lifecycleService);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        when(processedEventRepository.existsById("evt-003")).thenReturn(true);

        handler.handle(new AssetEvent(
                "evt-003",
                AssetEventHandler.STOCK_ISSUE_APPROVED,
                mapper.readTree("""
                        {"outwardId":"OUT-003","itemId":"ITEM-003","userId":"EMP-003",
                         "departmentName":"Engineering","issuedBy":"PROC-003","reusable":true}
                        """)));

        verify(processedEventRepository, never()).save(any());
        verifyNoInteractions(assetService, assignmentService, lifecycleService);
    }

    @Test
    void returnAcceptedClosesAssignment() throws Exception {
        when(processedEventRepository.existsById("evt-004")).thenReturn(false);

        handler.handle(new AssetEvent(
                "evt-004",
                AssetEventHandler.RETURN_ACCEPTED,
                mapper.readTree("""
                        {"assetId":"ASSET%2F26-27%2F001","returnId":"RET-001","condition":"GOOD"}
                        """)));

        verify(assignmentService).closeAssignment(
                eq("ASSET%2F26-27%2F001"), any(Instant.class), eq("RET-001"), eq(AssetCondition.GOOD));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}
