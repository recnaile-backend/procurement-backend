package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.exception.InsufficientStockException;
import com.recnaile.inventory.repository.*;
import com.recnaile.inventory.masterdata.MasterDataItemClient;
import com.recnaile.inventory.masterdata.MasterDataItemResponse;
import java.math.BigDecimal;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;

class InventoryEventServiceExtendedTest {

    @Mock StockBalanceRepository balances;
    @Mock StockMovementRepository movements;
    @Mock IdempotencyRepository idempotency;
    @Mock OutboxRepository outbox;
    @Mock InventoryAuditRepository audit;
    @Mock StockAllocationRepository allocations;
    @Mock MongoTemplate mongo;

    InventoryEventService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InventoryEventService(
                balances, movements, idempotency, outbox, audit, allocations,
                new ObjectMapper(), new SimpleMeterRegistry());

        when(idempotency.existsByEventId(anyString())).thenReturn(false);
        when(balances.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movements.save(any())).thenAnswer(i -> i.getArgument(0));
        when(audit.save(any())).thenAnswer(i -> i.getArgument(0));
        when(outbox.save(any())).thenAnswer(i -> i.getArgument(0));
        when(allocations.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void duplicateEventIsIgnoredAndNotPersistedAgain() throws Exception {
        when(idempotency.existsByEventId("event-1")).thenReturn(true);

        service.consume(new ObjectMapper().readTree("""
            {"eventId":"event-1","eventType":"GOODS_RECEIVED","payload":{}}
        """));

        verify(idempotency, never()).save(any());
        verifyNoInteractions(balances, movements, outbox, audit);
    }

    @Test
    void goodReturnAddsAvailableStock() throws Exception {
        StockBalanceDocument balance = balance("ITEM-1", 5, 0);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));
        StockAllocationDocument allocation = new StockAllocationDocument("OUT-1", "ITEM-1", balance.getBalanceKey(), 10, Instant.now());
        when(allocations.findByOutwardIdAndItemId("OUT-1", "ITEM-1")).thenReturn(Optional.of(allocation));

        service.consume(new ObjectMapper().readTree("""
            {"eventId":"event-return-1","eventType":"RETURN_RECORDED",
             "occurredAt":"2026-09-22T00:00:00Z","correlationId":"corr","traceId":"trace",
             "payload":{"returnId":"RETURN%2F26-27%2F001","outwardId":"OUT-1","itemId":"ITEM-1","qty":3,"condition":"GOOD","receivedBy":"user-1"}}
        """));

        assertEquals(8, balance.getAvailableQuantity());
        assertEquals(0, balance.getDefectQuantity());
        verify(movements).save(any(StockMovementDocument.class));
        verify(idempotency).save(any(IdempotencyDocument.class));
    }

    @Test
    void damagedReturnAddsDefectStock() throws Exception {
        StockBalanceDocument balance = balance("ITEM-1", 5, 1);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));
        StockAllocationDocument allocation = new StockAllocationDocument("OUT-2", "ITEM-1", balance.getBalanceKey(), 10, Instant.now());
        when(allocations.findByOutwardIdAndItemId("OUT-2", "ITEM-1")).thenReturn(Optional.of(allocation));

        service.consume(new ObjectMapper().readTree("""
            {"eventId":"event-return-2","eventType":"RETURN_RECORDED",
             "occurredAt":"2026-09-22T00:00:00Z","correlationId":"corr","traceId":"trace",
             "payload":{"returnId":"RETURN%2F26-27%2F002","outwardId":"OUT-2","itemId":"ITEM-1","qty":2,"condition":"DAMAGED","receivedBy":"user-1"}}
        """));

        assertEquals(5, balance.getAvailableQuantity());
        assertEquals(3, balance.getDefectQuantity());
    }

    @Test
    void invalidEventEnvelopeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.consume(new ObjectMapper().createObjectNode().put("eventType", "GOODS_RECEIVED")));

        verifyNoInteractions(balances, movements, idempotency, outbox, audit);
    }

    @Test
    void invalidReceivedQuantityIsRejected() throws Exception {
        var event = new ObjectMapper().readTree("""
            {"eventId":"event-invalid","eventType":"GOODS_RECEIVED",
             "payload":{"grnId":"GRN-1","itemId":"ITEM-1","acceptedQuantity":-1,"rejectedQuantity":0}}
        """);

        assertThrows(IllegalArgumentException.class, () -> service.consume(event));
        verify(balances, never()).save(any());
        verify(movements, never()).save(any());
    }

    @Test
    void optimisticLockingFailureIsPropagated() throws Exception {
        StockBalanceDocument balance = balance("ITEM-1", 5, 0);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));
        when(balances.save(any())).thenThrow(new OptimisticLockingFailureException("conflict"));

        var event = new ObjectMapper().readTree("""
            {"eventId":"event-concurrency","eventType":"OUTWARD_ISSUED",
             "occurredAt":"2026-09-22T00:00:00Z","correlationId":"corr","traceId":"trace",
             "payload":{"outwardId":"OUT-1","itemId":"ITEM-1","quantity":1,"issuedTo":"user-1"}}
        """);

        assertThrows(OptimisticLockingFailureException.class, () -> service.consume(event));
        verify(movements, never()).save(any());
        verify(idempotency, never()).save(any());
    }

    @Test
    void outwardIssuedConsumesDirectFinalOutwardContractAndCreatesAllocation() throws Exception {
        StockBalanceDocument balance = balance("ITEM-1", 10, 0);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));

        service.consume(new ObjectMapper().readTree("""
            {"eventId":"outward-event-1","eventType":"OUTWARD_ISSUED",
             "occurredAt":"2026-10-08T00:00:00Z","outwardNumber":"OUT%2F26-27%2F001",
             "departmentName":"IT","projectId":"PROJ-1","assetTracked":false,
             "items":[{"itemId":"ITEM-1","quantity":3,"assetTracked":false}]}
        """));

        assertEquals(7, balance.getAvailableQuantity());
        verify(allocations).save(any(StockAllocationDocument.class));
        verify(movements).save(any(StockMovementDocument.class));
    }

    @Test
    void goodsReceivedCanResolveCurrentInwardItemNameToCanonicalMasterDataItemId() throws Exception {
        MasterDataItemClient masterData = mock(MasterDataItemClient.class);
        MasterDataItemResponse laptop = new MasterDataItemResponse(
                "ITEM-1001", "Laptop", null, null, "IT", BigDecimal.TEN, BigDecimal.valueOf(50),
                BigDecimal.ZERO, BigDecimal.ZERO, "REUSABLE", "PCS", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "RACK-A01", "ACTIVE", null, "Electronics", true, null, 0L);
        when(masterData.findUniqueByName("Laptop")).thenReturn(Optional.of(laptop));
        when(masterData.findById("ITEM-1001")).thenReturn(Optional.of(laptop));
        InventoryEventService nameResolvingService = new InventoryEventService(
                balances, movements, idempotency, outbox, audit, allocations, new ObjectMapper(),
                new SimpleMeterRegistry(), masterData);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.empty());

        nameResolvingService.consume(new ObjectMapper().readTree("""
            {"eventId":"goods-name-1","eventType":"GOODS_RECEIVED",
             "payload":{"grnId":"GRN-100","itemName":"Laptop","acceptedQuantity":4,"rejectedQuantity":0}}
        """));

        verify(movements).save(argThat(m -> "ITEM-1001".equals(m.getItemId())));
    }

    @Test
    void returnRecordedRejectsUnknownCondition() throws Exception {
        StockBalanceDocument balance = balance("ITEM-1", 5, 0);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));
        StockAllocationDocument allocation = new StockAllocationDocument("OUT-3", "ITEM-1", balance.getBalanceKey(), 5, Instant.now());
        when(allocations.findByOutwardIdAndItemId("OUT-3", "ITEM-1")).thenReturn(Optional.of(allocation));

        assertThrows(IllegalArgumentException.class, () -> service.consume(new ObjectMapper().readTree("""
            {"eventId":"return-invalid-condition","eventType":"RETURN_RECORDED",
             "payload":{"returnId":"RETURN%2F26-27%2F003","outwardId":"OUT-3",
             "itemId":"ITEM-1","qty":1,"condition":"LOST"}}
        """)));

        verify(movements, never()).save(any());
    }

    @Test
    void returnRecordedCannotRestoreMoreThanIssued() throws Exception {
        StockBalanceDocument balance = balance("ITEM-1", 5, 0);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));
        StockAllocationDocument allocation = new StockAllocationDocument("OUT-4", "ITEM-1", balance.getBalanceKey(), 2, Instant.now());
        allocation.setReturnedQuantity(1);
        when(allocations.findByOutwardIdAndItemId("OUT-4", "ITEM-1")).thenReturn(Optional.of(allocation));

        assertThrows(IllegalArgumentException.class, () -> service.consume(new ObjectMapper().readTree("""
            {"eventId":"return-over","eventType":"RETURN_RECORDED",
             "payload":{"returnId":"RETURN%2F26-27%2F004","outwardId":"OUT-4",
             "itemId":"ITEM-1","qty":2,"condition":"GOOD"}}
        """)));
        verify(movements, never()).save(any());
    }

    private StockBalanceDocument balance(String itemId, int available, int defect) {
        StockBalanceDocument b = new StockBalanceDocument();
        b.setBalanceKey(InventoryEventService.key(itemId, null, null, null));
        b.setItemId(itemId);
        b.setAvailableQuantity(available);
        b.setDefectQuantity(defect);
        b.setMinimumStock(0);
        b.setMaximumStock(0);
        b.setLowStock(false);
        b.setStatus(available > 0 ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK);
        b.setUpdatedAt(Instant.now());
        return b;
    }
}
