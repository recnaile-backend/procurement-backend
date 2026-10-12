package com.recnaile.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

class ReportingLifecycleProjectionTest {
    private ReportingFactRepository facts;
    private ReportingProjectionRepository projections;
    private ReportingEventProcessor processor;

    @BeforeEach void setUp() {
        facts = mock(ReportingFactRepository.class);
        projections = mock(ReportingProjectionRepository.class);
        when(projections.findById(any())).thenReturn(Optional.empty());
        processor = new ReportingEventProcessor(new ObjectMapper(), facts, projections, new SimpleMeterRegistry());
    }

    @Test void goodsReceivedProjectsAcceptedAndRejectedQuantitiesAgainstGrnAndPo() {
        processor.process("""
          {"eventId":"grn-event-1","eventType":"GOODS_RECEIVED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"inward-service","correlationId":null,"traceId":null,"payload":{"grnId":"GRN-1","poId":"PO-1","itemName":"Keyboard","orderedQuantity":10,"acceptedQuantity":8,"rejectedQuantity":2,"unitOfMeasure":"EA","rejectionReason":null,"deliveryNoteNumber":null,"deliveryDate":null,"receivedAt":null,"receivedBy":"U-1","vendorName":null}}
          """);
        verify(facts).insert(ArgumentMatchers.<ReportingFact>argThat(f ->
            f.entityType().equals("INWARD") && f.entityId().equals("GRN-1") &&
            f.relatedEntityId().equals("PO-1") && f.quantity().equals(8.0) &&
            f.acceptedQuantity().equals(8.0) && f.rejectedQuantity().equals(2.0)));
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("INWARD:GRN-1") && p.relatedEntityId().equals("PO-1") &&
            p.quantity().equals(8.0) && p.acceptedQuantity().equals(8.0) &&
            p.rejectedQuantity().equals(2.0) && p.actorId().equals("U-1")));
    }

    @Test void lowStockAlertProjectsOnlyTheReportedAvailabilityAndThreshold() {
        processor.process("""
          {"eventId":"stock-event-1","eventType":"LOW_STOCK_ALERT","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"inventory-service","payload":{"itemId":"ITEM-1","availableQuantity":4,"reorderThreshold":10}}
          """);
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("STOCK:ITEM-1") && p.status().equals("LOW_STOCK") &&
            p.availableQuantity().equals(4.0) && p.reorderThreshold().equals(10.0)));
    }

    @Test void outwardIssueProjectionIncludesIssuedQuantityItemAndDepartment() {
        processor.process("""
          {"eventId":"out-event-1","eventType":"OUTWARD_ISSUED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"outward-service","payload":{"outwardId":"OUT-1","itemId":"ITEM-2","quantity":3,"issuedTo":"U-2","departmentId":"D-2","reusable":false}}
          """);
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("OUTWARD:OUT-1") && p.itemId().equals("ITEM-2") &&
            p.quantity().equals(3.0) && p.departmentId().equals("D-2") && p.actorId().equals("U-2")));
    }

    @Test void returnProjectionKeepsConditionQuantityAndOptionalAssetReference() {
        processor.process("""
          {"eventId":"return-event-1","eventType":"RETURN_RECORDED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"return-service","payload":{"returnId":"RETURN-1","itemId":"ITEM-3","quantity":1,"condition":"DAMAGED","returnedBy":"U-3","assetId":"ASSET-3"}}
          """);
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("RETURN:RETURN-1") && p.itemId().equals("ITEM-3") &&
            p.quantity().equals(1.0) && p.condition().equals("DAMAGED") &&
            p.relatedEntityId().equals("ASSET-3") && p.actorId().equals("U-3")));
    }

    @Test void assetStatusChangedProjectsLifecycleStatusAndActor() {
        processor.process("""
          {"eventId":"asset-event-1","eventType":"ASSET_STATUS_CHANGED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"asset-service","payload":{"assetId":"ASSET-4","previousStatus":"ACTIVE","newStatus":"DAMAGED","changedBy":"U-4"}}
          """);
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("ASSET:ASSET-4") && p.status().equals("DAMAGED") && p.actorId().equals("U-4")));
    }

    @Test void assetClosureRequestUpdatesTheAssetProjectionInsteadOfCreatingAnUnlinkedRequest() {
        processor.process("""
          {"eventId":"asset-close-event-1","eventType":"ASSET_CLOSURE_REQUESTED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"return-service","payload":{"assetId":"ASSET-5","returnId":"RETURN-5","reason":"Damaged"}}
          """);
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("ASSET:ASSET-5") && p.status().equals("CLOSURE_REQUESTED") &&
            p.relatedEntityId().equals("RETURN-5")));
    }

    @Test void rejectionFlaggedUpdatesTheExistingGrnProjectionWithoutLosingReceiptQuantities() {
        com.recnaile.reporting.model.ReportingProjection current = new com.recnaile.reporting.model.ReportingProjection(
            "INWARD:GRN-2", "INWARD", "GRN-2", "RECEIVED", null, null, "ITEM-6", null, "PO-2", null,
            9.0, null, 10.0, 9.0, 1.0, null, null, "U-6", "inward-service",
            java.time.Instant.parse("2026-10-10T00:00:00Z"), java.time.Instant.parse("2026-10-10T00:00:00Z"),
            java.time.Instant.parse("2026-10-10T00:00:00Z"), "grn-event-2", "GOODS_RECEIVED");
        when(projections.findById("INWARD:GRN-2")).thenReturn(Optional.of(current));

        processor.process("""
          {"eventId":"rejection-event-2","eventType":"REJECTION_FLAGGED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"inward-service","payload":{"grnId":"GRN-2","poId":"PO-2","itemId":"ITEM-6","rejectedQuantity":1,"reason":"Damaged"}}
          """);

        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("INWARD:GRN-2") && p.quantity().equals(9.0) &&
            p.rejectedQuantity().equals(1.0) && p.acceptedQuantity().equals(9.0) &&
            p.relatedEntityId().equals("PO-2")));
    }

    @Test void assetCreationRequestIsLinkedToTheOutwardRequest() {
        processor.process("""
          {"eventId":"asset-create-event-1","eventType":"ASSET_CREATION_REQUESTED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"outward-service","payload":{"outwardId":"OUT-8","itemId":"ITEM-8","assignedTo":"U-8","departmentId":"D-8"}}
          """);
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p ->
            p.id().equals("ASSET_REQUEST:asset-create-event-1") &&
            p.relatedEntityId().equals("OUT-8") && p.status().equals("CREATION_REQUESTED") &&
            p.itemId().equals("ITEM-8") && p.departmentId().equals("D-8") && p.actorId().equals("U-8")));
    }

}
