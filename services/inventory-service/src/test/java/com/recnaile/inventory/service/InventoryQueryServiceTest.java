package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.masterdata.MasterDataItemClient;
import com.recnaile.inventory.masterdata.MasterDataItemResponse;
import java.math.BigDecimal;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class InventoryQueryServiceTest {

    @Mock StockBalanceRepository balances;
    @Mock StockMovementRepository movements;
    @Mock MasterDataItemClient masterData;

    InventoryQueryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InventoryQueryService(balances, movements, masterData);
    }

    @Test
    void summaryAggregatesBalancesAndCalculatesLowStockStatus() {
        StockBalanceDocument first = balance("ITEM-1", 8, 1, 10, 50, true, StockStatus.LOW_STOCK);
        StockBalanceDocument second = balance("ITEM-1", 12, 0, 5, 30, false, StockStatus.IN_STOCK);
        when(balances.findByItemId("ITEM-1")).thenReturn(List.of(first, second));
        MasterDataItemResponse item = new MasterDataItemResponse(
                "ITEM-1", "Laptop", null, null, "IT", BigDecimal.TEN, BigDecimal.valueOf(50),
                BigDecimal.ZERO, BigDecimal.ZERO, "REUSABLE", "PCS", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "RACK-A01", "ACTIVE", null, "Electronics", true, null, 0L);
        when(masterData.findById("ITEM-1")).thenReturn(Optional.of(item));

        InventorySummaryResponse result = service.summary("ITEM-1");

        assertEquals("ITEM-1", result.itemId());
        assertEquals(20, result.availableQuantity());
        assertEquals(1, result.defectQuantity());
        assertEquals(21, result.totalQuantity());
        // Minimum/maximum are item-master properties, not sums of per-location balances.
        assertEquals(10, result.minimumStock());
        assertEquals(50, result.maximumStock());
        assertFalse(result.lowStock());
        assertEquals(StockStatus.IN_STOCK, result.status());
    }

    @Test
    void summaryReturnsOutOfStockWhenAvailableIsZero() {
        when(balances.findByItemId("ITEM-1"))
                .thenReturn(List.of(balance("ITEM-1", 0, 4, 10, 50, false, StockStatus.OUT_OF_STOCK)));
        MasterDataItemResponse item = new MasterDataItemResponse(
                "ITEM-1", "Laptop", null, null, "IT", BigDecimal.TEN, BigDecimal.valueOf(50),
                BigDecimal.ZERO, BigDecimal.ZERO, "REUSABLE", "PCS", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "RACK-A01", "ACTIVE", null, "Electronics", true, null, 0L);
        when(masterData.findById("ITEM-1")).thenReturn(Optional.of(item));

        InventorySummaryResponse result = service.summary("ITEM-1");

        assertEquals(0, result.availableQuantity());
        assertEquals(StockStatus.OUT_OF_STOCK, result.status());
        assertTrue(result.lowStock());
    }

    @Test
    void stockProjectionUsesMasterDataOpeningStockBeforeFirstInventoryMovement() {
        MasterDataItemResponse item = new MasterDataItemResponse(
                "ITEM-OPEN", "Pen", null, null, "Software", BigDecimal.TEN, BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.ONE, "REUSABLE", "PCS", BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, "RACK-003", "ACTIVE", "For Office Purpose", "Stationary", true, "QR-OPEN", 1L);
        StockBalanceDocument existing = balance("ITEM-OPEN", 0, 0, 10, 100, true, StockStatus.OUT_OF_STOCK);
        when(masterData.findById("ITEM-OPEN")).thenReturn(Optional.of(item));
        when(balances.findByItemId("ITEM-OPEN")).thenReturn(List.of(existing));
        when(movements.findByItemId("ITEM-OPEN")).thenReturn(List.of());

        var result = service.byItem("ITEM-OPEN");

        assertEquals(1, result.size());
        assertEquals(50, result.get(0).availableQuantity());
        assertEquals(1, result.get(0).defectQuantity());
        assertEquals(10, result.get(0).minimumStock());
        assertEquals(100, result.get(0).maximumStock());
        assertEquals(com.recnaile.inventory.domain.StockStatus.IN_STOCK, result.get(0).status());
    }

    @Test
    void summaryUsesMasterDataOpeningStockBeforeFirstInventoryMovement() {
        MasterDataItemResponse item = new MasterDataItemResponse(
                "ITEM-OPEN", "Pen", null, null, "Software", BigDecimal.TEN, BigDecimal.valueOf(100),
                BigDecimal.valueOf(50), BigDecimal.ONE, "REUSABLE", "PCS", BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, "RACK-003", "ACTIVE", "For Office Purpose", "Stationary", true, "QR-OPEN", 1L);
        StockBalanceDocument existing = balance("ITEM-OPEN", 0, 0, 10, 100, true, StockStatus.OUT_OF_STOCK);
        when(masterData.findById("ITEM-OPEN")).thenReturn(Optional.of(item));
        when(balances.findByItemId("ITEM-OPEN")).thenReturn(List.of(existing));
        when(movements.findByItemId("ITEM-OPEN")).thenReturn(List.of());

        var result = service.summary("ITEM-OPEN");

        assertEquals(50, result.availableQuantity());
        assertEquals(1, result.defectQuantity());
        assertFalse(result.lowStock());
        assertEquals(StockStatus.IN_STOCK, result.status());
    }

    @Test
    void movementsByItemDelegatesToRepositoryQueryInNewestFirstOrder() {
        List<StockMovementDocument> expected = List.of(new StockMovementDocument());

        when(movements.findByItemIdOrderByOccurredAtDesc("ITEM-1")).thenReturn(expected);

        List<StockMovementDocument> result = service.movementsByItem("ITEM-1");

        assertSame(expected, result);
        verify(movements).findByItemIdOrderByOccurredAtDesc("ITEM-1");
    }

    @Test
    void stockProjectionUsesMasterDataMetadataAndShowsNewItemAtZeroStock() {
        MasterDataItemResponse item = new MasterDataItemResponse(
                "ITEM-NEW", "Laptop", "2026-10-08T00:00:00Z", "2026-10-08T00:00:00Z",
                "IT", BigDecimal.TEN, BigDecimal.valueOf(50), BigDecimal.ZERO, BigDecimal.ZERO,
                "REUSABLE", "PCS", BigDecimal.valueOf(50000), BigDecimal.valueOf(18), null,
                "RACK-A01", "ACTIVE", "Office laptop", "Electronics", true, "QR-1", 0L);
        when(masterData.findAll()).thenReturn(List.of(item));
        when(balances.findAll()).thenReturn(List.of());

        InventoryQueryService productionService = new InventoryQueryService(balances, movements, masterData);
        var result = productionService.allStocks();

        assertEquals(1, result.size());
        assertEquals("ITEM-NEW", result.get(0).itemId());
        assertEquals("Laptop", result.get(0).itemName());
        assertEquals("IT", result.get(0).departmentName());
        assertEquals("RACK-A01", result.get(0).location());
        assertEquals(10, result.get(0).minimumStock());
        assertEquals(50, result.get(0).maximumStock());
        assertEquals(0, result.get(0).availableQuantity());
        assertEquals("ACTIVE", result.get(0).masterDataStatus());
    }

    private StockBalanceDocument balance(String itemId, int available, int defect,
            int minimum, int maximum, boolean low, StockStatus status) {
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
