package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.exception.InsufficientStockException;
import com.recnaile.inventory.repository.*;
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
    @Mock MongoTemplate mongo;

    InventoryEventService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InventoryEventService(
                balances, movements, idempotency, outbox, audit, mongo,
                new ObjectMapper(), new SimpleMeterRegistry());

        when(idempotency.existsByEventId(anyString())).thenReturn(false);
        when(balances.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movements.save(any())).thenAnswer(i -> i.getArgument(0));
        when(audit.save(any())).thenAnswer(i -> i.getArgument(0));
        when(outbox.save(any())).thenAnswer(i -> i.getArgument(0));
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

        service.consume(new ObjectMapper().readTree("""
            {"eventId":"event-return-1","eventType":"RETURN_RECORDED",
             "occurredAt":"2026-09-22T00:00:00Z","correlationId":"corr","traceId":"trace",
             "payload":{"returnId":"RET-1","itemId":"ITEM-1","quantity":3,"condition":"GOOD","receivedBy":"user-1"}}
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

        service.consume(new ObjectMapper().readTree("""
            {"eventId":"event-return-2","eventType":"RETURN_RECORDED",
             "occurredAt":"2026-09-22T00:00:00Z","correlationId":"corr","traceId":"trace",
             "payload":{"returnId":"RET-2","itemId":"ITEM-1","quantity":2,"condition":"DAMAGED","receivedBy":"user-1"}}
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

    private StockBalanceDocument balance(String itemId, double available, double defect) {
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
