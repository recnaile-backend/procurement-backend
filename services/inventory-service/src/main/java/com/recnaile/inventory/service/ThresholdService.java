package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.InventoryAuditDocument;
import com.recnaile.inventory.domain.OutboxEventDocument;
import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.repository.InventoryAuditRepository;
import com.recnaile.inventory.repository.OutboxRepository;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.masterdata.MasterDataItemClient;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThresholdService {
    private final StockBalanceRepository balances;
    private final InventoryAuditRepository auditRepository;
    private final OutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;
    private final MasterDataItemClient masterDataItemClient;

    @Autowired
    public ThresholdService(StockBalanceRepository balances, InventoryAuditRepository auditRepository,
            OutboxRepository outboxRepository, MeterRegistry meterRegistry, MasterDataItemClient masterDataItemClient) {
        this.balances = balances;
        this.auditRepository = auditRepository;
        this.outboxRepository = outboxRepository;
        this.meterRegistry = meterRegistry;
        this.masterDataItemClient = masterDataItemClient;
    }

    public ThresholdService(StockBalanceRepository balances, InventoryAuditRepository auditRepository,
            OutboxRepository outboxRepository, MeterRegistry meterRegistry) {
        this(balances, auditRepository, outboxRepository, meterRegistry, null);
    }

    @Transactional
    public StockBalanceDocument update(String itemId, String departmentName, String projectId, String location,
            int minimum, int maximum, String actorId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        var masterItem = masterDataItemClient == null ? null : masterDataItemClient.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found in Master Data: " + itemId));
        if (masterItem != null && masterItem.status() != null && !"ACTIVE".equalsIgnoreCase(masterItem.status())) {
            throw new IllegalStateException("Inactive Master Data item cannot be changed in Inventory: " + itemId);
        }
        if (minimum < 0 || maximum < 0 || maximum < minimum) {
            throw new IllegalArgumentException("Invalid min/max stock thresholds");
        }

        String effectiveDepartment = departmentName == null || departmentName.isBlank()
                ? masterItem == null ? departmentName : masterItem.department() : departmentName;
        String effectiveLocation = location == null || location.isBlank()
                ? masterItem == null ? location : masterItem.rack() : location;
        String key = InventoryEventService.key(itemId, effectiveDepartment, projectId, effectiveLocation);
        StockBalanceDocument b = balances.findByBalanceKey(key).orElseGet(StockBalanceDocument::new);
        b.setBalanceKey(key);
        b.setItemId(itemId);
        b.setDepartmentName(effectiveDepartment);
        b.setProjectId(projectId);
        b.setLocation(effectiveLocation);

        int beforeAvailable = b.getAvailableQuantity();
        int beforeMinimum = b.getMinimumStock();
        int beforeMaximum = b.getMaximumStock();
        StockStatus beforeStatus = b.getStatus();
        boolean beforeLow = b.isLowStock();

        b.setMinimumStock(minimum);
        b.setMaximumStock(maximum);
        b.setThresholdsOverridden(true);
        b.setLowStock(b.getAvailableQuantity() <= minimum);
        b.setStatus(b.getAvailableQuantity() <= 0 ? StockStatus.OUT_OF_STOCK
                : b.isLowStock() ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK);
        b.setUpdatedAt(Instant.now());
        StockBalanceDocument saved = balances.save(b);

        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        auditRepository.save(new InventoryAuditDocument(
                "STOCK_BALANCE", key, "THRESHOLDS_UPDATED", eventId, actorId, null, now, null, null,
                Map.of("availableQuantity", beforeAvailable, "minimumStock", beforeMinimum,
                        "maximumStock", beforeMaximum, "lowStock", beforeLow,
                        "status", beforeStatus == null ? StockStatus.OUT_OF_STOCK.name() : beforeStatus.name()),
                Map.of("availableQuantity", saved.getAvailableQuantity(), "minimumStock", saved.getMinimumStock(),
                        "maximumStock", saved.getMaximumStock(), "lowStock", saved.isLowStock(),
                        "status", saved.getStatus().name())));

        if (!beforeLow && saved.isLowStock()) {
            Map<String, Object> payload = Map.of(
                    "itemId", itemId,
                    "availableQuantity", saved.getAvailableQuantity(),
                    "reorderThreshold", saved.getMinimumStock());
            outboxRepository.save(new OutboxEventDocument(
                    UUID.randomUUID().toString(), "LOW_STOCK_ALERT", 1, now, "inventory-service",
                    null, null, payload));
            meterRegistry.counter("inventory.low_stock.alerts").increment();
        }
        return saved;
    }
}
