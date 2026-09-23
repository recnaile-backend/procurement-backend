package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.repository.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

class InventoryEventServiceTest {
    StockBalanceRepository balances;
    StockMovementRepository movements;
    IdempotencyRepository idempotency;
    OutboxRepository outbox;
    InventoryAuditRepository audit;
    InventoryEventService service;

    @BeforeEach
    void setUp() {
        balances=mock(StockBalanceRepository.class);
        movements=mock(StockMovementRepository.class);
        idempotency=mock(IdempotencyRepository.class);
        outbox=mock(OutboxRepository.class);
        audit=mock(InventoryAuditRepository.class);
        MongoTemplate mongo=mock(MongoTemplate.class);
        service=new InventoryEventService(balances,movements,idempotency,outbox,audit,mongo,
            new ObjectMapper(),new SimpleMeterRegistry());
        when(idempotency.existsByEventId(anyString())).thenReturn(false);
        when(balances.save(any())).thenAnswer(i->i.getArgument(0));
        when(movements.save(any())).thenAnswer(i->i.getArgument(0));
        when(audit.save(any())).thenAnswer(i->i.getArgument(0));
        when(outbox.save(any())).thenAnswer(i->i.getArgument(0));
    }

    @Test
    void goodsReceivedAddsAcceptedAndRejected() throws Exception {
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.empty());
        var event=new ObjectMapper().readTree("""
          {"eventId":"11111111-1111-1111-1111-111111111111","eventType":"GOODS_RECEIVED",
           "occurredAt":"2026-09-22T00:00:00Z","correlationId":"22222222-2222-2222-2222-222222222222",
           "traceId":"trace","payload":{"grnId":"GRN-1","poId":"PO-1","itemId":"ITEM-1",
           "acceptedQuantity":10,"rejectedQuantity":2,"receivedBy":"user-1"}}
        """);
        service.consume(event);
        verify(movements,times(2)).save(any(StockMovementDocument.class));
        verify(idempotency).save(any(IdempotencyDocument.class));
    }

    @Test
    void outwardCannotMakeAvailableNegative() throws Exception {
        StockBalanceDocument b=new StockBalanceDocument();
        b.setBalanceKey(InventoryEventService.key("ITEM-1",null,null,null));
        b.setItemId("ITEM-1"); b.setAvailableQuantity(3); b.setMinimumStock(0); b.setDefectQuantity(0);
        b.setLowStock(false); b.setStatus(StockStatus.IN_STOCK);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(b));
        var event=new ObjectMapper().readTree("""
          {"eventId":"33333333-3333-3333-3333-333333333333","eventType":"OUTWARD_ISSUED",
           "occurredAt":"2026-09-22T00:00:00Z","correlationId":"44444444-4444-4444-4444-444444444444",
           "traceId":"trace","payload":{"outwardId":"OUT-1","itemId":"ITEM-1","quantity":4,
           "issuedTo":"user-1","reusable":false}}
        """);
        assertThrows(com.recnaile.inventory.exception.InsufficientStockException.class,()->service.consume(event));
        verify(movements,never()).save(any());
    }
}
