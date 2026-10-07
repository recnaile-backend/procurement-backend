package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.recnaile.inventory.domain.InventoryAuditDocument;
import com.recnaile.inventory.domain.OutboxEventDocument;
import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.repository.InventoryAuditRepository;
import com.recnaile.inventory.repository.OutboxRepository;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.OptimisticLockingFailureException;

class InventoryAdjustmentServiceTest {
    @Mock StockBalanceRepository balances;
    @Mock StockMovementRepository movements;
    @Mock InventoryAuditRepository audit;
    @Mock OutboxRepository outbox;

    InventoryAdjustmentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InventoryAdjustmentService(
                balances, movements, audit, new SimpleMeterRegistry(), outbox);
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

        StockBalanceDocument result = service.updateAvailableStock(
                "ITEM-1", null, null, null, 5, "Physical count", "user-1");

        assertEquals(5, result.getAvailableQuantity());
        assertEquals(StockStatus.LOW_STOCK, result.getStatus());
        verify(movements).save(any(StockMovementDocument.class));
        verify(audit).save(any(InventoryAuditDocument.class));
        verify(outbox).save(any(OutboxEventDocument.class));
    }

    @Test
    void adjustmentDoesNotCreateMovementWhenTargetEqualsCurrent() {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setBalanceKey(InventoryEventService.key("ITEM-1", null, null, null));
        balance.setItemId("ITEM-1");
        balance.setAvailableQuantity(10);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(balance));

        service.updateAvailableStock("ITEM-1", null, null, null, 10, "No change", "user-1");

        verify(balances, never()).save(any());
        verifyNoInteractions(movements, audit, outbox);
    }
}
