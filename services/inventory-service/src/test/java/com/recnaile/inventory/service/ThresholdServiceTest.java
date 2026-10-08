package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.InventoryAuditRepository;
import com.recnaile.inventory.repository.OutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ThresholdServiceTest {

    @Mock StockBalanceRepository balances;
    @Mock InventoryAuditRepository audit;
    @Mock OutboxRepository outbox;
    ThresholdService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ThresholdService(balances, audit, outbox, new SimpleMeterRegistry());
        when(balances.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(audit.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outbox.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateCreatesBalanceAndCalculatesLowStockStatus() {
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.empty());

        StockBalanceDocument result =
                service.update("ITEM-1", "IT", "PROJ-1", "RACK-A", 10, 50, "tester");

        assertEquals("ITEM-1", result.getItemId());
        assertEquals("IT", result.getDepartmentName());
        assertEquals("PROJ-1", result.getProjectId());
        assertEquals("RACK-A", result.getLocation());
        assertEquals(10, result.getMinimumStock());
        assertEquals(50, result.getMaximumStock());
        assertEquals(0, result.getAvailableQuantity());
        assertEquals(StockStatus.OUT_OF_STOCK, result.getStatus());
        assertTrue(result.isLowStock());
        verify(balances).save(result);
    }

    @Test
    void updateExistingBalanceMarksItLowWhenAvailableIsAtThreshold() {
        StockBalanceDocument existing = new StockBalanceDocument();
        existing.setBalanceKey(InventoryEventService.key("ITEM-1", "IT", null, null));
        existing.setItemId("ITEM-1");
        existing.setDepartmentName("IT");
        existing.setAvailableQuantity(10);
        existing.setDefectQuantity(2);
        existing.setStatus(StockStatus.IN_STOCK);

        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(existing));

        StockBalanceDocument result =
                service.update("ITEM-1", "IT", null, null, 10, 50, "tester");

        assertTrue(result.isLowStock());
        assertEquals(StockStatus.LOW_STOCK, result.getStatus());
        assertEquals(10, result.getAvailableQuantity());
        assertEquals(2, result.getDefectQuantity());
    }

    @Test
    void thresholdUpdateCreatesAuditAndLowStockOutboxWhenThresholdCrossesCurrentStock() {
        StockBalanceDocument existing = new StockBalanceDocument();
        existing.setBalanceKey(InventoryEventService.key("ITEM-1", null, null, null));
        existing.setItemId("ITEM-1");
        existing.setAvailableQuantity(5);
        existing.setDefectQuantity(0);
        existing.setMinimumStock(2);
        existing.setMaximumStock(20);
        existing.setLowStock(false);
        existing.setStatus(StockStatus.IN_STOCK);
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(existing));

        service.update("ITEM-1", null, null, null, 5, 20, "tester");

        verify(audit).save(any(InventoryAuditDocument.class));
        verify(outbox).save(any(OutboxEventDocument.class));
    }

    @Test
    void updateRejectsMaximumBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> service.update("ITEM-1", null, null, null, 50, 10, "tester"));

        verify(balances, never()).save(any());
    }

    @Test
    void updateRejectsNegativeThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> service.update("ITEM-1", null, null, null, -1, 10, "tester"));

        verify(balances, never()).save(any());
    }
}
