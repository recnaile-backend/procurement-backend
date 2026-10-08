package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.dto.InventoryStockResponse;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.masterdata.MasterDataItemClient;
import com.recnaile.inventory.masterdata.MasterDataItemResponse;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryQueryService {
    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final MasterDataItemClient masterData;

    /** Production constructor. */
    @Autowired
    public InventoryQueryService(StockBalanceRepository balances,
                                 StockMovementRepository movements,
                                 MasterDataItemClient masterData) {
        this.balances = balances;
        this.movements = movements;
        this.masterData = masterData;
    }

    /** Test/backward-compatible constructor. */
    public InventoryQueryService(StockBalanceRepository balances,
                                 StockMovementRepository movements) {
        this(balances, movements, null);
    }

    /**
     * Inventory does not create item master records. The list is projected
     * from Master Data and overlaid with Inventory-owned stock balances.
     * Therefore a newly-created Master Data item is visible here even before
     * the first GRN arrives, with zero stock.
     */
    public List<InventoryStockResponse> allStocks() {
        List<MasterDataItemResponse> masterItems = masterData == null ? List.of() : masterData.findAll();
        List<StockBalanceDocument> existing = balances.findAll();
        Map<String, List<StockBalanceDocument>> byItem = existing.stream()
                .collect(Collectors.groupingBy(StockBalanceDocument::getItemId));

        List<InventoryStockResponse> result = new ArrayList<>();
        for (MasterDataItemResponse item : masterItems) {
            List<StockBalanceDocument> itemBalances = byItem.getOrDefault(item.itemId(), List.of());
            if (itemBalances.isEmpty()) {
                result.add(toResponse(zeroBalance(item), item, true));
            } else {
                boolean useMasterOpeningStock = itemBalances.size() == 1 && movements.findByItemId(item.itemId()).isEmpty();
                itemBalances.stream()
                        .sorted(Comparator.comparing(StockBalanceDocument::getBalanceKey))
                        .map(balance -> toResponse(balance, item, useMasterOpeningStock))
                        .forEach(result::add);
            }
        }

        // Keep orphaned historical balances visible rather than silently hiding stock.
        Map<String, MasterDataItemResponse> known = masterItems.stream()
                .filter(i -> i.itemId() != null)
                .collect(Collectors.toMap(MasterDataItemResponse::itemId, Function.identity(), (a, b) -> a));
        existing.stream()
                .filter(balance -> !known.containsKey(balance.getItemId()))
                .sorted(Comparator.comparing(StockBalanceDocument::getBalanceKey))
                .map(balance -> toResponse(balance, null, false))
                .forEach(result::add);
        return result;
    }

    public List<InventoryStockResponse> byItem(String itemId) {
        validateItemId(itemId);
        MasterDataItemResponse item = masterData == null ? null : masterData.findById(itemId).orElse(null);
        List<StockBalanceDocument> itemBalances = balances.findByItemId(itemId);
        if (itemBalances.isEmpty()) {
            if (item == null) {
                throw new IllegalArgumentException("Item not found in Master Data: " + itemId);
            }
            return List.of(toResponse(zeroBalance(item), item, true));
        }
        boolean useMasterOpeningStock = itemBalances.size() == 1 && movements.findByItemId(itemId).isEmpty();
        return itemBalances.stream()
                .sorted(Comparator.comparing(StockBalanceDocument::getBalanceKey))
                .map(balance -> toResponse(balance, item, useMasterOpeningStock))
                .toList();
    }

    public List<StockMovementDocument> movementsByItem(String itemId) {
        validateItemId(itemId);
        return movements.findByItemIdOrderByOccurredAtDesc(itemId);
    }

    public InventorySummaryResponse summary(String itemId) {
        validateItemId(itemId);
        MasterDataItemResponse item = masterData == null ? null : masterData.findById(itemId).orElse(null);
        if (item == null && masterData != null) {
            throw new IllegalArgumentException("Item not found in Master Data: " + itemId);
        }

        List<StockBalanceDocument> stock = balances.findByItemId(itemId);
        boolean hasMovements = !movements.findByItemId(itemId).isEmpty();
        int available = stock.stream().mapToInt(StockBalanceDocument::getAvailableQuantity).sum();
        int defect = stock.stream().mapToInt(StockBalanceDocument::getDefectQuantity).sum();
        // Master Data stock fields are treated as opening stock only. Once an
        // Inventory movement exists, the Inventory ledger remains authoritative.
        if (!hasMovements && stock.size() <= 1 && item != null) {
            available = masterStockValue(item.availableStock());
            defect = masterStockValue(item.defectStock());
        }

        int minimum = item != null && item.minimumStock() != null
                ? item.minimumStock().intValueExact() : stock.stream().mapToInt(StockBalanceDocument::getMinimumStock).max().orElse(0);
        int maximum = item != null && item.maximumStock() != null
                ? item.maximumStock().intValueExact() : stock.stream().mapToInt(StockBalanceDocument::getMaximumStock).max().orElse(0);

        boolean lowStock = available <= minimum;
        StockStatus status = available <= 0 ? StockStatus.OUT_OF_STOCK
                : lowStock ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK;
        return new InventorySummaryResponse(itemId, available, defect, available + defect,
                minimum, maximum, lowStock, status);
    }

    private InventoryStockResponse toResponse(StockBalanceDocument balance, MasterDataItemResponse item, boolean useMasterOpeningStock) {
        String department = firstNonBlank(balance.getDepartmentName(), item == null ? null : item.department());
        String location = firstNonBlank(balance.getLocation(), item == null ? null : item.rack());
        int minimum = effectiveThreshold(balance.getMinimumStock(), balance.isThresholdsOverridden(), item == null ? null : item.minimumStock());
        int maximum = effectiveThreshold(balance.getMaximumStock(), balance.isThresholdsOverridden(), item == null ? null : item.maximumStock());
        int available = balance.getAvailableQuantity();
        int defect = balance.getDefectQuantity();
        if (useMasterOpeningStock && item != null) {
            available = masterStockValue(item.availableStock());
            defect = masterStockValue(item.defectStock());
        }
        boolean low = available <= minimum;
        StockStatus status = available <= 0 ? StockStatus.OUT_OF_STOCK
                : low ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK;

        return new InventoryStockResponse(
                balance.getBalanceKey(),
                balance.getItemId(),
                item == null ? "Unknown item" : firstNonBlank(item.name(), "Unnamed item"),
                item == null ? "UNKNOWN" : firstNonBlank(item.category(), "UNKNOWN"),
                item == null ? "UNKNOWN" : firstNonBlank(item.unitOfMeasure(), "UNKNOWN"),
                item == null ? "UNKNOWN" : firstNonBlank(item.type(), "UNKNOWN"),
                item != null && item.assetTracked(),
                item == null || item.defaultUnitPrice() == null ? BigDecimal.ZERO : item.defaultUnitPrice(),
                item == null || item.gstRate() == null ? BigDecimal.ZERO : item.gstRate(),
                item == null ? "" : firstNonBlank(item.barcodeQrIdentifier(), ""),
                firstNonBlank(department, "UNASSIGNED"),
                firstNonBlank(balance.getProjectId(), "UNASSIGNED"),
                firstNonBlank(location, "UNASSIGNED"),
                item == null ? "UNASSIGNED" : firstNonBlank(item.rack(), "UNASSIGNED"),
                item == null ? "" : firstNonBlank(item.remark(), ""),
                available,
                defect,
                minimum,
                maximum,
                low,
                status,
                item == null ? "NOT_FOUND" : firstNonBlank(item.status(), "UNKNOWN"),
                balance.getVersion() == null ? 0L : balance.getVersion(),
                balance.getUpdatedAt());
    }

    private StockBalanceDocument zeroBalance(MasterDataItemResponse item) {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setBalanceKey(InventoryEventService.key(item.itemId(), item.department(), null, item.rack()));
        balance.setItemId(item.itemId());
        balance.setDepartmentName(item.department());
        balance.setLocation(item.rack());
        balance.setAvailableQuantity(0);
        balance.setDefectQuantity(0);
        balance.setMinimumStock(item.minimumStock() == null ? 0 : item.minimumStock().intValue());
        balance.setMaximumStock(item.maximumStock() == null ? 0 : item.maximumStock().intValue());
        balance.setThresholdsOverridden(false);
        balance.setLowStock(balance.getAvailableQuantity() <= balance.getMinimumStock());
        balance.setStatus(StockStatus.OUT_OF_STOCK);
        balance.setUpdatedAt(java.time.Instant.now());
        return balance;
    }

    private int masterStockValue(BigDecimal value) {
        if (value == null) return 0;
        return value.intValueExact();
    }

    private int effectiveThreshold(int balanceValue, boolean overridden, BigDecimal masterValue) {
        if (overridden) return balanceValue;
        return masterValue == null ? balanceValue : masterValue.intValue();
    }

    private void validateItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId is required");
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
