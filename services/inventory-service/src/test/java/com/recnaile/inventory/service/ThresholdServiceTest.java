package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.repository.StockBalanceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ThresholdServiceTest {

    @Mock StockBalanceRepository balances;
    ThresholdService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ThresholdService(balances);
        when(balances.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateCreatesBalanceAndCalculatesLowStockStatus() {
        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.empty());

        StockBalanceDocument result =
                service.update("ITEM-1", "DEPT-1", "PROJ-1", "RACK-A", 10, 50);

        assertEquals("ITEM-1", result.getItemId());
        assertEquals("DEPT-1", result.getDepartmentId());
        assertEquals("PROJ-1", result.getProjectId());
        assertEquals("RACK-A", result.getLocation());
        assertEquals(10, result.getMinimumStock());
        assertEquals(50, result.getMaximumStock());
        assertEquals(0, result.getAvailableQuantity());
        assertEquals(StockStatus.OUT_OF_STOCK, result.getStatus());
        assertFalse(result.isLowStock());
        verify(balances).save(result);
    }

    @Test
    void updateExistingBalanceMarksItLowWhenAvailableIsAtThreshold() {
        StockBalanceDocument existing = new StockBalanceDocument();
        existing.setBalanceKey(InventoryEventService.key("ITEM-1", "DEPT-1", null, null));
        existing.setItemId("ITEM-1");
        existing.setDepartmentId("DEPT-1");
        existing.setAvailableQuantity(10);
        existing.setDefectQuantity(2);
        existing.setStatus(StockStatus.IN_STOCK);

        when(balances.findByBalanceKey(anyString())).thenReturn(Optional.of(existing));

        StockBalanceDocument result =
                service.update("ITEM-1", "DEPT-1", null, null, 10, 50);

        assertTrue(result.isLowStock());
        assertEquals(StockStatus.LOW_STOCK, result.getStatus());
        assertEquals(10, result.getAvailableQuantity());
        assertEquals(2, result.getDefectQuantity());
    }

    @Test
    void updateRejectsMaximumBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> service.update("ITEM-1", null, null, null, 50, 10));

        verify(balances, never()).save(any());
    }

    @Test
    void updateRejectsNegativeThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> service.update("ITEM-1", null, null, null, -1, 10));

        verify(balances, never()).save(any());
    }
}
