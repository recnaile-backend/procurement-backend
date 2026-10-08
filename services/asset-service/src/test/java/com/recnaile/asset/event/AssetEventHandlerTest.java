package com.recnaile.asset.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.ProcessedEventRepository;
import com.recnaile.asset.service.AssetAssignmentService;
import com.recnaile.asset.service.AssetService;
import com.recnaile.asset.service.AssetReturnService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetEventHandlerTest {

    private AssetService assetService;
    private AssetAssignmentService assignmentService;
    private AssetReturnService returnService;
    private ProcessedEventRepository processedEventRepository;
    private AssetEventHandler handler;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        assetService = mock(AssetService.class);
        assignmentService = mock(AssetAssignmentService.class);
        returnService = mock(AssetReturnService.class);
        processedEventRepository = mock(ProcessedEventRepository.class);

        handler = new AssetEventHandler(
                assetService,
                assignmentService,
                returnService,
                processedEventRepository,
                Clock.fixed(Instant.parse("2026-10-06T08:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void stockIssueApprovedCreatesAndAssignsReusableAsset() throws Exception {
        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"),
                "ITEM-001", "OUT%2F26-27%2F001", "Laptop", "Brand", "Model",
                "PROC-001", AssetStatus.RETURNED, AssetCondition.GOOD, null,
                Instant.parse("2026-10-06T08:00:00Z"));

        when(processedEventRepository.existsById("evt-001")).thenReturn(false);
        when(assetService.getByOutwardNumberAndItem("OUT%2F26-27%2F001", "ITEM-001")).thenReturn(java.util.List.of());
        when(assetService.create(eq("ITEM-001"), eq("OUT%2F26-27%2F001"), eq("Laptop"), eq("Brand"),
                eq("Model"), eq("PROC-001"), eq(AssetCondition.GOOD), isNull(), any(Instant.class)))
                .thenReturn(asset);

        handler.handle(new AssetEvent(
                "evt-001",
                AssetEventHandler.STOCK_ISSUE_APPROVED,
                mapper.readTree("""
                        {
                          "outwardNumber":"OUT%2F26-27%2F001",
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
                eq("PROC-001"), eq("OUT%2F26-27%2F001"), any(Instant.class), isNull());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }


    @Test
    void outwardIssuedCreatesAndAssignsReusableUnitsFromFinalOutwardContract() throws Exception {
        Asset first = Asset.create(
                new AssetId("ASSET%2F26-27%2F010"),
                "ITEM-001", "OUT%2F26-27%2F001", "ITEM-001", null, null,
                "TL-001", AssetStatus.RETURNED, AssetCondition.GOOD, "Laptop issue for project work",
                Instant.parse("2026-10-06T08:00:00Z"));
        Asset second = Asset.create(
                new AssetId("ASSET%2F26-27%2F011"),
                "ITEM-001", "OUT%2F26-27%2F001", "ITEM-001", null, null,
                "TL-001", AssetStatus.RETURNED, AssetCondition.GOOD, "Laptop issue for project work",
                Instant.parse("2026-10-06T08:00:00Z"));

        when(processedEventRepository.existsById("evt-outward-001")).thenReturn(false);
        when(assetService.getByOutwardNumberAndItem("OUT%2F26-27%2F001", "ITEM-001"))
                .thenReturn(java.util.List.of());
        when(assetService.create(eq("ITEM-001"), eq("OUT%2F26-27%2F001"), eq("ITEM-001"),
                isNull(), isNull(), eq("TL-001"), eq(AssetCondition.GOOD),
                eq("Laptop issue for project work"), any(Instant.class))).thenReturn(first, second);

        handler.handle(new AssetEvent(
                "evt-outward-001",
                AssetEventHandler.OUTWARD_ISSUED,
                mapper.readTree("""
                        {
                          "outwardNumber":"OUT%2F26-27%2F001",
                          "issuerId":"TL-001",
                          "departmentName":"Engineering",
                          "recipientEmployeeId":"EMP-1001",
                          "purpose":"Laptop issue for project work",
                          "items":[
                            {"itemId":"ITEM-001","quantity":2,"assetTracked":true}
                          ]
                        }
                        """)));

        verify(assignmentService).startAssignment(
                eq("ASSET%2F26-27%2F010"), eq("EMP-1001"), eq("Engineering"),
                eq("TL-001"), eq("OUT%2F26-27%2F001"), any(Instant.class), eq("Laptop issue for project work"));
        verify(assignmentService).startAssignment(
                eq("ASSET%2F26-27%2F011"), eq("EMP-1001"), eq("Engineering"),
                eq("TL-001"), eq("OUT%2F26-27%2F001"), any(Instant.class), eq("Laptop issue for project work"));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void outwardIssuedIgnoresNonReusableItems() throws Exception {
        when(processedEventRepository.existsById("evt-outward-002")).thenReturn(false);

        handler.handle(new AssetEvent(
                "evt-outward-002",
                AssetEventHandler.OUTWARD_ISSUED,
                mapper.readTree("""
                        {
                          "outwardNumber":"OUT%2F26-27%2F002",
                          "issuerId":"TL-001",
                          "departmentName":"Engineering",
                          "recipientEmployeeId":"EMP-1002",
                          "items":[
                            {"itemId":"ITEM-002","quantity":10,"assetTracked":false}
                          ]
                        }
                        """)));

        verifyNoInteractions(assetService, assignmentService, returnService);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void nonReusableIssueIsIgnoredAndMarkedProcessed() throws Exception {
        when(processedEventRepository.existsById("evt-002")).thenReturn(false);

        handler.handle(new AssetEvent(
                "evt-002",
                AssetEventHandler.STOCK_ISSUE_APPROVED,
                mapper.readTree("""
                        {"outwardNumber":"OUT%2F26-27%2F002","itemId":"ITEM-002","reusable":false}
                        """)));

        verifyNoInteractions(assetService, assignmentService, returnService);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        when(processedEventRepository.existsById("evt-003")).thenReturn(true);

        handler.handle(new AssetEvent(
                "evt-003",
                AssetEventHandler.STOCK_ISSUE_APPROVED,
                mapper.readTree("""
                        {"outwardNumber":"OUT%2F26-27%2F003","itemId":"ITEM-003","userId":"EMP-003",
                         "departmentName":"Engineering","issuedBy":"PROC-003","reusable":true}
                        """)));

        verify(processedEventRepository, never()).save(any());
        verifyNoInteractions(assetService, assignmentService, returnService);
    }


    @Test
    void returnRecordedDelegatesOutwardItemQuantityToAssetReturnService() throws Exception {
        when(processedEventRepository.existsById("evt-return-001")).thenReturn(false);

        handler.handle(new AssetEvent(
                "evt-return-001",
                AssetEventHandler.RETURN_RECORDED,
                mapper.readTree("""
                        {
                          "returnId":"RETURN%2F26-27%2F001",
                          "outwardNumber":"OUT%2F26-27%2F001",
                          "itemId":"ITEM-001",
                          "qty":2,
                          "condition":"GOOD"
                        }
                        """)));

        verify(returnService).processReturnForOutwardItem(
                eq("OUT%2F26-27%2F001"), eq("ITEM-001"), eq(2), eq("RETURN%2F26-27%2F001"),
                eq(AssetCondition.GOOD), any(Instant.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void returnAcceptedClosesAssignment() throws Exception {
        when(processedEventRepository.existsById("evt-004")).thenReturn(false);

        handler.handle(new AssetEvent(
                "evt-004",
                AssetEventHandler.RETURN_ACCEPTED,
                mapper.readTree("""
                        {"assetId":"ASSET%2F26-27%2F001","outwardNumber":"OUT%2F26-27%2F001","returnId":"RETURN%2F26-27%2F001","condition":"GOOD"}
                        """)));

        verify(returnService).processReturn(
                eq("ASSET%2F26-27%2F001"), eq("OUT%2F26-27%2F001"), eq("RETURN%2F26-27%2F001"), eq(AssetCondition.GOOD), any(Instant.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}
