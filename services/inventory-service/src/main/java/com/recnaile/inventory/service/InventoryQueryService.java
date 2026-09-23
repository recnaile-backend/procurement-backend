package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InventoryQueryService {
    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;

    public InventoryQueryService(StockBalanceRepository balances, StockMovementRepository movements) {
        this.balances = balances;
        this.movements = movements;
    }

    public List<StockBalanceDocument> allStocks() {
        return balances.findAll();
    }

    public List<StockBalanceDocument> byItem(String itemId) {
        return balances.findByItemId(itemId);
    }

    public List<StockMovementDocument> movementsByItem(String itemId) {
        return movements.findByItemIdOrderByOccurredAtDesc(itemId);
    }

    public InventorySummaryResponse summary(String itemId) {
        List<StockBalanceDocument> stock = balances.findByItemId(itemId);
        int available = stock.stream().mapToInt(StockBalanceDocument::getAvailableQuantity).sum();
        int defect = stock.stream().mapToInt(StockBalanceDocument::getDefectQuantity).sum();
        int minimum = stock.stream().mapToInt(StockBalanceDocument::getMinimumStock).sum();
        int maximum = stock.stream().mapToInt(StockBalanceDocument::getMaximumStock).sum();
        boolean lowStock = stock.stream().anyMatch(StockBalanceDocument::isLowStock);

        com.recnaile.inventory.domain.StockStatus status;
        if (available <= 0) {
            status = com.recnaile.inventory.domain.StockStatus.OUT_OF_STOCK;
        } else if (lowStock) {
            status = com.recnaile.inventory.domain.StockStatus.LOW_STOCK;
        } else {
            status = com.recnaile.inventory.domain.StockStatus.IN_STOCK;
        }

        return new InventorySummaryResponse(itemId, available, defect, available + defect,
                minimum, maximum, lowStock, status);
    }
}
