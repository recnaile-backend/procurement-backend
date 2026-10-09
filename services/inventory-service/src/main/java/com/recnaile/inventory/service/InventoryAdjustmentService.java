package com.recnaile.inventory.service;

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
import com.recnaile.inventory.masterdata.MasterDataItemClient;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAdjustmentService {
    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final InventoryAuditRepository auditRepository;
    private final MeterRegistry meterRegistry;
    private final OutboxRepository outboxRepository;
    private final ManualAdjustmentIdempotencyRepository idempotencyRepository;
    private final MasterDataItemClient masterDataItemClient;

    @Autowired
    public InventoryAdjustmentService(
            StockBalanceRepository balances,
            StockMovementRepository movements,
            InventoryAuditRepository auditRepository,
            MeterRegistry meterRegistry,
            OutboxRepository outboxRepository,
            ManualAdjustmentIdempotencyRepository idempotencyRepository,
            MasterDataItemClient masterDataItemClient) {
        this.balances = balances;
        this.movements = movements;
        this.auditRepository = auditRepository;
        this.meterRegistry = meterRegistry;
        this.outboxRepository = outboxRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.masterDataItemClient = masterDataItemClient;
    }

    public InventoryAdjustmentService(StockBalanceRepository balances, StockMovementRepository movements,
            InventoryAuditRepository auditRepository, MeterRegistry meterRegistry, OutboxRepository outboxRepository,
            ManualAdjustmentIdempotencyRepository idempotencyRepository) {
        this(balances, movements, auditRepository, meterRegistry, outboxRepository, idempotencyRepository, null);
    }

    @Transactional
    public StockBalanceDocument applyAdjustment(
            String itemId, String departmentName, String projectId, String location,
            StockType stockType, int delta, String reason, String actorId, String idempotencyKey) {
        if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId is required");
        var masterItem = masterDataItemClient == null ? null : masterDataItemClient.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in Master Data: " + itemId));
        if (masterItem != null && masterItem.status() != null && !"ACTIVE".equalsIgnoreCase(masterItem.status())) {
            throw new IllegalStateException("Inactive Master Data item cannot be changed in Inventory: " + itemId);
        }
        if (stockType == null) throw new IllegalArgumentException("stockType is required");
        String key = normalizeKey(idempotencyKey);
        if (delta == 0) throw new IllegalArgumentException("delta must not be zero");

        String effectiveDepartment = departmentName == null || departmentName.isBlank()
                ? masterItem == null ? departmentName : masterItem.department() : departmentName;
        String effectiveLocation = location == null || location.isBlank()
                ? masterItem == null ? location : masterItem.rack() : location;
        String balanceKey = InventoryEventService.key(itemId, effectiveDepartment, projectId, effectiveLocation);
        String fingerprint = fingerprintDelta(itemId, effectiveDepartment, projectId, effectiveLocation, stockType, delta, reason, actorId);
        var existing = idempotencyRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            if (!existing.get().getFingerprint().equals(fingerprint)) {
                throw new IllegalArgumentException("Idempotency-Key was already used for a different stock adjustment");
            }
            return balances.findByBalanceKey(balanceKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotent stock adjustment balance no longer exists"));
        }

        StockBalanceDocument balance = balances.findByBalanceKey(balanceKey).orElseGet(StockBalanceDocument::new);
        balance.setBalanceKey(balanceKey);
        balance.setItemId(itemId);
        balance.setDepartmentName(effectiveDepartment);
        balance.setProjectId(projectId);
        balance.setLocation(effectiveLocation);

        int beforeAvailable = balance.getAvailableQuantity();
        int beforeDefect = balance.getDefectQuantity();
        int afterAvailable = beforeAvailable;
        int afterDefect = beforeDefect;
        if (stockType == StockType.AVAILABLE) afterAvailable += delta;
        else afterDefect += delta;
        if (afterAvailable < 0) throw new com.recnaile.inventory.exception.InsufficientStockException(itemId, Math.abs(delta), beforeAvailable);
        if (afterDefect < 0) throw new IllegalArgumentException("Defect stock cannot become negative");

        balance.setAvailableQuantity(afterAvailable);
        balance.setDefectQuantity(afterDefect);
        recalculateStatus(balance);
        Instant now = Instant.now();
        balance.setUpdatedAt(now);
        balances.save(balance);

        String eventId = "MANUAL_ADJUSTMENT:" + key;
        String adjustmentReason = reason == null || reason.isBlank() ? "MANUAL_STOCK_ADJUSTMENT" : reason.trim();
        movements.save(new StockMovementDocument(
                eventId, "MANUAL_STOCK_ADJUSTMENT", eventId, itemId, effectiveDepartment, projectId, effectiveLocation,
                stockType, Math.abs(delta), delta, adjustmentReason, now, now, null, null, actorId));

        auditRepository.save(new InventoryAuditDocument(
                "STOCK_BALANCE", balanceKey, "STOCK_ADJUSTED", eventId, actorId, null, now, null, null,
                Map.of("availableQuantity", beforeAvailable, "defectQuantity", beforeDefect),
                Map.of("availableQuantity", afterAvailable, "defectQuantity", afterDefect, "status", balance.getStatus().name())));

        idempotencyRepository.save(new ManualAdjustmentIdempotencyDocument(key, fingerprint, balanceKey, afterAvailable, now));
        meterRegistry.counter("inventory.stock.manual_adjustments", "stockType", stockType.name()).increment();
        if (afterAvailable <= balance.getMinimumStock()) {
            outboxRepository.save(new OutboxEventDocument(
                    UUID.randomUUID().toString(), "LOW_STOCK_ALERT", 1, now, "inventory-service", null, null,
                    Map.of("itemId", itemId, "availableQuantity", afterAvailable, "reorderThreshold", balance.getMinimumStock())));
        }
        return balance;
    }

    private String fingerprintDelta(String itemId, String departmentName, String projectId, String location,
                                    StockType stockType, int delta, String reason, String actorId) {
        String input = String.join("|", String.valueOf(itemId), String.valueOf(departmentName),
                String.valueOf(projectId), String.valueOf(location), stockType.name(), String.valueOf(delta),
                String.valueOf(reason), String.valueOf(actorId));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to fingerprint stock adjustment", ex);
        }
    }

    private void recalculateStatus(StockBalanceDocument balance) {
        boolean low = balance.getAvailableQuantity() <= balance.getMinimumStock();
        balance.setLowStock(low);
        balance.setStatus(balance.getAvailableQuantity() <= 0
                ? StockStatus.OUT_OF_STOCK
                : low ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK);
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
        String normalized = key.trim();
        if (normalized.length() > 200) throw new IllegalArgumentException("Idempotency-Key must not exceed 200 characters");
        return normalized;
    }


}
