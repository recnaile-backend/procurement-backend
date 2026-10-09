package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.recnaile.inventory.domain.InventoryAuditDocument;
import com.recnaile.inventory.domain.ManualAdjustmentIdempotencyDocument;
import com.recnaile.inventory.domain.OutboxEventDocument;
import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.domain.StockType;
import com.recnaile.inventory.repository.InventoryAuditRepository;
import com.recnaile.inventory.repository.ManualAdjustmentIdempotencyRepository;
import com.recnaile.inventory.repository.OutboxRepository;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class InventoryAdjustmentServiceTest {
    @Mock StockBalanceRepository balances;
    @Mock StockMovementRepository movements;
    @Mock InventoryAuditRepository audit;
    @Mock OutboxRepository outbox;
    @Mock ManualAdjustmentIdempotencyRepository idempotency;

    InventoryAdjustmentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InventoryAdjustmentService(
                balances, movements, audit, new SimpleMeterRegistry(), outbox, idempotency);
        when(balances.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(movements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(audit.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outbox.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void adjustmentRecordsMovementAuditAndLowStockAlert() {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setBalanceKey(InventoryEventService.key("ITEM-1", null, null, null));
        balance.setItemId("ITEM-1");
        balance.setAvailableQuantity(10);
        balance.setDefectQuantity(0);
        balance.setMinimumStock(5);
        balance.setMaximumStock(50);
        balance.setLowStock(false);
        balance.setStatus(StockStatus.IN_STOCK);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));
        when(idempotency.findByIdempotencyKey("adjustment-1")).thenReturn(Optional.empty());

        StockBalanceDocument result = service.applyAdjustment(
                "ITEM-1", null, null, null, StockType.AVAILABLE, -5,
                "Physical count", "user-1", "adjustment-1");

        assertEquals(5, result.getAvailableQuantity());
        assertEquals(StockStatus.LOW_STOCK, result.getStatus());
        verify(movements).save(any(StockMovementDocument.class));
        verify(audit).save(any(InventoryAuditDocument.class));
        verify(outbox).save(any(OutboxEventDocument.class));
        verify(idempotency).save(any(ManualAdjustmentIdempotencyDocument.class));
    }

    @Test
    void defectAdjustmentCanIncreaseDefectStock() {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setBalanceKey(InventoryEventService.key("ITEM-1", null, null, null));
        balance.setItemId("ITEM-1");
        balance.setAvailableQuantity(10);
        balance.setDefectQuantity(1);
        balance.setMinimumStock(2);
        balance.setMaximumStock(50);
        balance.setStatus(StockStatus.IN_STOCK);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));
        when(idempotency.findByIdempotencyKey("adjustment-2")).thenReturn(Optional.empty());

        StockBalanceDocument result = service.applyAdjustment(
                "ITEM-1", null, null, null, StockType.DEFECT, 3,
                "Damage found during count", "user-1", "adjustment-2");

        assertEquals(10, result.getAvailableQuantity());
        assertEquals(4, result.getDefectQuantity());
        verify(movements).save(any(StockMovementDocument.class));
    }
}
