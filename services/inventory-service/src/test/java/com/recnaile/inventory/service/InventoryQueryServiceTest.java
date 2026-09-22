package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

class InventoryQueryServiceTest {

    @Mock StockBalanceRepository balances;
    @Mock StockMovementRepository movements;

    InventoryQueryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InventoryQueryService(balances, movements);
    }

    @Test
    void summaryAggregatesBalancesAndCalculatesLowStockStatus() {
        StockBalanceDocument first = balance("ITEM-1", 8, 1, 10, 50, true, StockStatus.LOW_STOCK);
        StockBalanceDocument second = balance("ITEM-1", 12, 0, 5, 30, false, StockStatus.IN_STOCK);
        when(balances.findByItemId("ITEM-1")).thenReturn(List.of(first, second));

        InventorySummaryResponse result = service.summary("ITEM-1");

        assertEquals("ITEM-1", result.itemId());
        assertEquals(20, result.availableQuantity());
        assertEquals(1, result.defectQuantity());
        assertEquals(21, result.totalQuantity());
        assertEquals(15, result.minimumStock());
        assertEquals(80, result.maximumStock());
        assertTrue(result.lowStock());
        assertEquals(StockStatus.LOW_STOCK, result.status());
    }

    @Test
    void summaryReturnsOutOfStockWhenAvailableIsZero() {
        when(balances.findByItemId("ITEM-1"))
                .thenReturn(List.of(balance("ITEM-1", 0, 4, 10, 50, false, StockStatus.OUT_OF_STOCK)));

        InventorySummaryResponse result = service.summary("ITEM-1");

        assertEquals(0, result.availableQuantity());
        assertEquals(StockStatus.OUT_OF_STOCK, result.status());
    }

    @Test
    void movementsByItemDelegatesToPaginatedRepositoryQuery() {
        Pageable pageable = PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<StockMovementDocument> expected = new PageImpl<>(List.of());

        when(movements.findByItemIdOrderByOccurredAtDesc("ITEM-1", pageable)).thenReturn(expected);

        Page<StockMovementDocument> result = service.movementsByItem("ITEM-1", pageable);

        assertSame(expected, result);
        verify(movements).findByItemIdOrderByOccurredAtDesc("ITEM-1", pageable);
    }

    private StockBalanceDocument balance(String itemId, double available, double defect,
            double minimum, double maximum, boolean low, StockStatus status) {
        StockBalanceDocument b = new StockBalanceDocument();
        b.setBalanceKey(InventoryEventService.key(itemId, null, null, null));
        b.setItemId(itemId);
        b.setAvailableQuantity(available);
        b.setDefectQuantity(defect);
        b.setMinimumStock(minimum);
        b.setMaximumStock(maximum);
        b.setLowStock(low);
        b.setStatus(status);
        b.setUpdatedAt(Instant.now());
        return b;
    }
}
