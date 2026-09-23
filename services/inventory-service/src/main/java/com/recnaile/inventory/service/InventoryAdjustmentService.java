package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.InventoryAuditDocument;
import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.domain.StockType;
import com.recnaile.inventory.exception.InsufficientStockException;
import com.recnaile.inventory.repository.InventoryAuditRepository;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAdjustmentService {
    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final InventoryAuditRepository auditRepository;
    private final MeterRegistry meterRegistry;

    public InventoryAdjustmentService(
            StockBalanceRepository balances,
            StockMovementRepository movements,
            InventoryAuditRepository auditRepository,
            MeterRegistry meterRegistry) {
        this.balances = balances;
        this.movements = movements;
        this.auditRepository = auditRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public StockBalanceDocument updateAvailableStock(
            String itemId,
            String departmentName,
            String projectId,
            String location,
            int targetAvailable,
            String reason,
            String actorId) {

        if (targetAvailable < 0) {
            throw new IllegalArgumentException("availableQuantity must be >= 0");
        }

        String key = InventoryEventService.key(itemId, departmentName, projectId, location);
        StockBalanceDocument balance = balances.findByBalanceKey(key)
                .orElseGet(StockBalanceDocument::new);

        balance.setBalanceKey(key);
        balance.setItemId(itemId);
        balance.setDepartmentName(departmentName);
        balance.setProjectId(projectId);
        balance.setLocation(location);

        int before = balance.getAvailableQuantity();
        int delta = targetAvailable - before;

        if (delta == 0) {
            return balance;
        }

        // An adjustment is still represented in the immutable ledger. This keeps
        // the current balance auditable instead of silently overwriting it.
        if (targetAvailable < 0) {
            throw new InsufficientStockException(itemId, Math.abs(delta), before);
        }

        boolean wasLow = balance.isLowStock();
        balance.setAvailableQuantity(targetAvailable);
        recalculateStatus(balance);
        balance.setUpdatedAt(Instant.now());

        try {
            balances.save(balance);
        } catch (OptimisticLockingFailureException ex) {
            meterRegistry.counter("inventory.stock.concurrency_conflict").increment();
            throw ex;
        }

        String eventId = UUID.randomUUID().toString();
        String sourceId = "MANUAL-ADJUSTMENT-" + eventId;
        String adjustmentReason = (reason == null || reason.isBlank())
                ? "MANUAL_STOCK_ADJUSTMENT"
                : reason.trim();

        StockMovementDocument movement = new StockMovementDocument(
                eventId,
                "MANUAL_STOCK_ADJUSTMENT",
                sourceId,
                itemId,
                departmentName,
                projectId,
                location,
                StockType.AVAILABLE,
                Math.abs(delta),
                delta,
                adjustmentReason,
                Instant.now(),
                Instant.now(),
                null,
                null,
                actorId);
        movements.save(movement);

        Map<String, Object> beforeState = Map.of(
                "availableQuantity", before,
                "defectQuantity", balance.getDefectQuantity(),
                "status", statusName(before, balance.getMinimumStock()));
        Map<String, Object> afterState = Map.of(
                "availableQuantity", balance.getAvailableQuantity(),
                "defectQuantity", balance.getDefectQuantity(),
                "status", balance.getStatus().name());

        auditRepository.save(new InventoryAuditDocument(
                "STOCK_BALANCE",
                balance.getBalanceKey(),
                "AVAILABLE_STOCK_ADJUSTED",
                eventId,
                actorId,
                null,
                Instant.now(),
                null,
                null,
                beforeState,
                afterState));

        meterRegistry.counter("inventory.stock.manual_adjustments").increment();

        if (!wasLow && balance.isLowStock()) {
            meterRegistry.counter("inventory.low_stock.alerts").increment();
        }

        return balance;
    }

    private void recalculateStatus(StockBalanceDocument balance) {
        boolean low = balance.getAvailableQuantity() > 0
                && balance.getAvailableQuantity() <= balance.getMinimumStock();
        balance.setLowStock(low);
        balance.setStatus(balance.getAvailableQuantity() <= 0
                ? StockStatus.OUT_OF_STOCK
                : low ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK);
    }

    private String statusName(int available, int minimum) {
        if (available <= 0) return StockStatus.OUT_OF_STOCK.name();
        if (available <= minimum) return StockStatus.LOW_STOCK.name();
        return StockStatus.IN_STOCK.name();
    }
}
