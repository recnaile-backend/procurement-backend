package com.recnaile.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.exception.InsufficientStockException;
import com.recnaile.inventory.repository.*;
import com.recnaile.inventory.masterdata.MasterDataItemClient;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authoritative Inventory event processor.
 *
 * Inventory owns the stock ledger. Upstream services only publish business
 * facts; they never modify this service's MongoDB directly.
 */
@Service
public class InventoryEventService {
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final InventoryAuditRepository auditRepository;
    private final StockAllocationRepository allocationRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final MasterDataItemClient masterDataItemClient;

    @Autowired
    public InventoryEventService(
            StockBalanceRepository balanceRepository,
            StockMovementRepository movementRepository,
            IdempotencyRepository idempotencyRepository,
            OutboxRepository outboxRepository,
            InventoryAuditRepository auditRepository,
            StockAllocationRepository allocationRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            MasterDataItemClient masterDataItemClient) {
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
        this.auditRepository = auditRepository;
        this.allocationRepository = allocationRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.masterDataItemClient = masterDataItemClient;
    }

    /** Test/backward-compatible constructor. */
    public InventoryEventService(StockBalanceRepository balanceRepository,
            StockMovementRepository movementRepository, IdempotencyRepository idempotencyRepository,
            OutboxRepository outboxRepository, InventoryAuditRepository auditRepository,
            StockAllocationRepository allocationRepository, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this(balanceRepository, movementRepository, idempotencyRepository, outboxRepository, auditRepository,
                allocationRepository, objectMapper, meterRegistry, null);
    }

    /**
     * The unique movement index is the final concurrency/idempotency guard.
     * The listener treats a duplicate-key failure as an already-processed
     * Kafka delivery. Different events still propagate optimistic-locking
     * failures so Kafka can redeliver them.
     */
    @Transactional
    public void consume(JsonNode event) {
        String eventId = required(event, "eventId");
        String eventType = required(event, "eventType");

        if (idempotencyRepository.existsByEventId(eventId)) {
            meterRegistry.counter("inventory.events.duplicate", "eventType", eventType).increment();
            return;
        }

        switch (eventType) {
            case "GOODS_RECEIVED" -> goodsReceived(event);
            case "OUTWARD_ISSUED" -> outwardIssued(event);
            case "RETURN_RECORDED" -> returnRecorded(event);
            default -> meterRegistry.counter("inventory.events.ignored", "eventType", eventType).increment();
        }

        idempotencyRepository.save(new IdempotencyDocument(eventId, eventType, Instant.now()));
    }

    private void goodsReceived(JsonNode event) {
        JsonNode payload = payload(event);

        JsonNode items = payload.get("items");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                String itemId = resolveItemId(item);
                int accepted = firstInteger(item, "acceptedQuantity", "acceptedQty", "quantity", "receivedQuantity");
                int rejected = firstInteger(item, "rejectedQuantity", "rejectedQty", "defectQuantity");
                validateNonNegative(accepted, "acceptedQuantity");
                validateNonNegative(rejected, "rejectedQuantity");
                if (accepted > 0) apply(event, itemId, requiredSource(payload, "grnId", "inwardNumber", "sourceId"),
                        StockType.AVAILABLE, accepted, "GOODS_RECEIVED", dimensions(payload, item));
                if (rejected > 0) apply(event, itemId, requiredSource(payload, "grnId", "inwardNumber", "sourceId"),
                        StockType.DEFECT, rejected, "GOODS_RECEIVED_REJECTED", dimensions(payload, item));
            }
            return;
        }

        String itemId = resolveItemId(payload);
        String sourceId = requiredSource(payload, "grnId", "inwardNumber", "sourceId");
        int accepted = firstInteger(payload, "acceptedQuantity", "acceptedQty", "quantity", "receivedQuantity");
        int rejected = firstInteger(payload, "rejectedQuantity", "rejectedQty", "defectQuantity");
        validateNonNegative(accepted, "acceptedQuantity");
        validateNonNegative(rejected, "rejectedQuantity");
        if (accepted == 0 && rejected == 0) {
            throw new IllegalArgumentException("GOODS_RECEIVED must contain an accepted or rejected quantity");
        }
        if (accepted > 0) apply(event, itemId, sourceId, StockType.AVAILABLE, accepted,
                "GOODS_RECEIVED", dimensions(payload, payload));
        if (rejected > 0) apply(event, itemId, sourceId, StockType.DEFECT, rejected,
                "GOODS_RECEIVED_REJECTED", dimensions(payload, payload));
    }

    private void outwardIssued(JsonNode event) {
        JsonNode payload = payload(event);
        String outwardNumber = firstText(payload, "outwardNumber", "outwardId");
        if (outwardNumber == null || outwardNumber.isBlank()) {
            throw new IllegalArgumentException("OUTWARD_ISSUED requires outwardNumber");
        }

        JsonNode items = payload.get("items");
        if (items != null && items.isArray()) {
            if (items.isEmpty()) throw new IllegalArgumentException("OUTWARD_ISSUED requires at least one item");
            for (JsonNode item : items) {
                String itemId = required(item, "itemId");
                int quantity = integralQuantity(item.get("quantity"), "quantity");
                if (quantity <= 0) throw new IllegalArgumentException("Issued quantity must be greater than zero");
                Dimensions dimensions = dimensions(payload, item);
                StockBalanceDocument issuedBalance = apply(event, itemId, outwardNumber, StockType.AVAILABLE, -quantity,
                        "OUTWARD_ISSUED", dimensions);
                // getOrCreate may fill the rack/location from Master Data. Persist the
                // actual balance key that was debited, not the incomplete event dimensions.
                upsertAllocation(outwardNumber, itemId, quantity, issuedBalance.getBalanceKey(), Instant.now());
            }
            return;
        }

        // Compatibility with the older single-item event contract.
        String itemId = required(payload, "itemId");
        int quantity = firstInteger(payload, "quantity", "issuedQuantity");
        if (quantity <= 0) throw new IllegalArgumentException("Issued quantity must be greater than zero");
        Dimensions dimensions = dimensions(payload, payload);
        StockBalanceDocument issuedBalance = apply(event, itemId, outwardNumber, StockType.AVAILABLE, -quantity,
                "OUTWARD_ISSUED", dimensions);
        upsertAllocation(outwardNumber, itemId, quantity, issuedBalance.getBalanceKey(), Instant.now());
    }

    private void returnRecorded(JsonNode event) {
        JsonNode payload = payload(event);
        String itemId = required(payload, "itemId");
        String outwardId = firstText(payload, "outwardNumber", "outwardId");
        if (outwardId == null || outwardId.isBlank()) {
            throw new IllegalArgumentException("RETURN_RECORDED requires outwardNumber/outwardId");
        }
        String returnId = required(payload, "returnId");
        int quantity = firstInteger(payload, "qty", "quantity");
        if (quantity <= 0) throw new IllegalArgumentException("Return quantity must be greater than zero");

        String condition = required(payload, "condition").trim().toUpperCase(Locale.ROOT);
        StockType stockType = switch (condition) {
            case "GOOD" -> StockType.AVAILABLE;
            case "DAMAGED" -> StockType.DEFECT;
            default -> throw new IllegalArgumentException("Unsupported return condition: " + condition);
        };

        // A returnId is the business identity of a return. This protects stock from
        // being restored twice when a normal Return Service event and an administrative
        // Asset recovery event describe the same return but carry different Kafka eventIds.
        List<StockMovementDocument> priorReturnMovements = movementRepository
                .findBySourceTransactionIdAndItemIdAndEventType(returnId, itemId, "RETURN_RECORDED");
        if (!priorReturnMovements.isEmpty()) {
            boolean sameReturn = priorReturnMovements.stream().allMatch(movement ->
                    movement.getQuantity() == quantity
                    && movement.getStockType() == stockType
                    && ("RETURN_" + condition).equals(movement.getReason()));
            if (!sameReturn) {
                throw new IllegalArgumentException(
                        "Return ID was already processed with different quantity or condition: " + returnId);
            }
            return;
        }

        StockAllocationDocument allocation = allocationRepository
                .findByOutwardIdAndItemId(outwardId, itemId)
                .orElseGet(() -> migrateAllocationFromIssueLedger(outwardId, itemId));

        if (allocation.getRemainingQuantity() < quantity) {
            throw new IllegalArgumentException("Return quantity exceeds the remaining issued quantity for outward " + outwardId);
        }

        StockBalanceDocument balance = balanceRepository.findByBalanceKey(allocation.getBalanceKey())
                .orElseGet(() -> repairAllocationBalanceKeyFromIssueLedger(allocation, outwardId, itemId));

        applyToBalance(event, balance, returnId, stockType, quantity, "RETURN_" + condition);
        allocation.recordReturn(quantity, Instant.now());
        allocationRepository.save(allocation);
    }

    /**
     * Repairs legacy allocations that stored event dimensions before Master Data
     * supplied the canonical rack/location. The issue movement records the exact
     * balance dimensions actually debited, so use it only when it resolves to one
     * existing balance. Never guess by item alone.
     */
    private StockBalanceDocument repairAllocationBalanceKeyFromIssueLedger(
            StockAllocationDocument allocation, String outwardId, String itemId) {
        List<StockMovementDocument> issueMovements = movementRepository
                .findBySourceTransactionIdAndItemIdAndEventType(outwardId, itemId, "OUTWARD_ISSUED");
        Set<String> candidateKeys = new LinkedHashSet<>();
        for (StockMovementDocument movement : issueMovements) {
            candidateKeys.add(key(itemId, movement.getDepartmentName(), movement.getProjectId(), movement.getLocation()));
        }
        List<StockBalanceDocument> candidates = candidateKeys.stream()
                .map(balanceRepository::findByBalanceKey)
                .flatMap(Optional::stream)
                .toList();
        if (candidates.size() != 1) {
            throw new IllegalStateException("Cannot safely resolve the issued inventory balance for outward "
                    + outwardId + " and item " + itemId + "; allocation key=" + allocation.getBalanceKey()
                    + ", matching balances=" + candidates.size());
        }
        StockBalanceDocument repaired = candidates.get(0);
        allocation.setBalanceKey(repaired.getBalanceKey());
        allocation.setUpdatedAt(Instant.now());
        allocationRepository.save(allocation);
        return repaired;
    }

    private StockAllocationDocument migrateAllocationFromIssueLedger(String outwardId, String itemId) {
        List<StockMovementDocument> movements = movementRepository
                .findBySourceTransactionIdAndItemIdAndEventType(outwardId, itemId, "OUTWARD_ISSUED");
        if (movements.isEmpty()) {
            throw new IllegalArgumentException("No issued inventory allocation exists for outward " + outwardId + " and item " + itemId);
        }

        int issued = movements.stream().mapToInt(m -> Math.abs(m.getDelta())).sum();
        String balanceKey = movements.get(0).getSourceTransactionId() == null
                ? null : InventoryEventService.key(itemId, movements.get(0).getDepartmentName(),
                        movements.get(0).getProjectId(), movements.get(0).getLocation());
        if (balanceKey == null || balanceKey.isBlank()) {
            throw new IllegalStateException("Issued inventory movement has no balance dimension for outward " + outwardId);
        }
        StockAllocationDocument allocation = new StockAllocationDocument(
                outwardId, itemId, balanceKey, issued, Instant.now());
        try {
            return allocationRepository.save(allocation);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            return allocationRepository.findByOutwardIdAndItemId(outwardId, itemId)
                    .orElseThrow(() -> duplicate);
        }
    }

    private void upsertAllocation(String outwardId, String itemId, int quantity, String balanceKey, Instant now) {
        StockAllocationDocument allocation = allocationRepository
                .findByOutwardIdAndItemId(outwardId, itemId)
                .orElse(null);
        if (allocation == null) {
            allocationRepository.save(new StockAllocationDocument(outwardId, itemId, balanceKey, quantity, now));
            return;
        }
        if (!Objects.equals(allocation.getBalanceKey(), balanceKey)) {
            throw new IllegalStateException("Outward allocation changed balance dimension for " + outwardId + "/" + itemId);
        }
        if (allocation.getIssuedQuantity() != quantity) {
            throw new IllegalStateException("Duplicate OUTWARD_ISSUED event has a different issued quantity for " + outwardId + "/" + itemId);
        }
    }

    private StockBalanceDocument apply(JsonNode event, String itemId, String sourceId, StockType type,
                       int delta, String reason, Dimensions dimensions) {
        StockBalanceDocument balance = getOrCreate(itemId, dimensions.departmentName,
                dimensions.projectId, dimensions.location);
        applyToBalance(event, balance, sourceId, type, delta, reason);
        return balance;
    }

    private void applyToBalance(JsonNode event, StockBalanceDocument balance, String sourceId,
                                StockType type, int delta, String reason) {
        int beforeAvailable = balance.getAvailableQuantity();
        int beforeDefect = balance.getDefectQuantity();
        int newAvailable = beforeAvailable;
        int newDefect = beforeDefect;
        if (type == StockType.AVAILABLE) newAvailable = beforeAvailable + delta;
        else newDefect = beforeDefect + delta;

        if (newAvailable < 0) {
            meterRegistry.counter("inventory.stock.rejected", "reason", "INSUFFICIENT_AVAILABLE").increment();
            throw new InsufficientStockException(balance.getItemId(), Math.abs(delta), beforeAvailable);
        }
        if (newDefect < 0) {
            throw new IllegalArgumentException("Defect stock cannot become negative");
        }

        boolean wasLow = balance.isLowStock();
        balance.setAvailableQuantity(newAvailable);
        balance.setDefectQuantity(newDefect);
        recalculateStatus(balance);
        balance.setUpdatedAt(Instant.now());
        try {
            balanceRepository.save(balance);
        } catch (OptimisticLockingFailureException ex) {
            meterRegistry.counter("inventory.stock.concurrency_conflict").increment();
            throw ex;
        }

        String eventId = required(event, "eventId");
        String itemId = balance.getItemId();
        StockMovementDocument movement = new StockMovementDocument(
                eventId + ":" + itemId + ":" + type,
                eventId,
                required(event, "eventType"),
                sourceId,
                itemId,
                balance.getDepartmentName(),
                balance.getProjectId(),
                balance.getLocation(),
                type,
                Math.abs(delta),
                delta,
                reason,
                instant(event, "occurredAt"),
                Instant.now(),
                text(event, "correlationId"),
                text(event, "traceId"),
                firstText(payload(event), "receivedBy", "issuedBy", "returnByEmpId"));
        movementRepository.save(movement);

        Map<String, Object> before = Map.of(
                "availableQuantity", beforeAvailable,
                "defectQuantity", beforeDefect,
                "status", statusName(beforeAvailable, balance.getMinimumStock()));
        Map<String, Object> after = Map.of(
                "availableQuantity", newAvailable,
                "defectQuantity", newDefect,
                "status", balance.getStatus().name());
        auditRepository.save(new InventoryAuditDocument(
                "STOCK_BALANCE", balance.getBalanceKey(), "STOCK_CHANGED", eventId,
                firstText(payload(event), "receivedBy", "issuedBy", "returnByEmpId"), null,
                Instant.now(), text(event, "correlationId"), text(event, "traceId"), before, after));

        meterRegistry.counter("inventory.stock.movements", "type", type.name()).increment();
        if (!wasLow && balance.isLowStock()) {
            Map<String, Object> alertPayload = Map.of(
                    "itemId", itemId,
                    "availableQuantity", newAvailable,
                    "reorderThreshold", balance.getMinimumStock());
            outboxRepository.save(new OutboxEventDocument(
                    UUID.randomUUID().toString(), "LOW_STOCK_ALERT", 1, Instant.now(),
                    "inventory-service", text(event, "correlationId"), text(event, "traceId"), alertPayload));
            meterRegistry.counter("inventory.low_stock.alerts").increment();
        }
    }

    private StockBalanceDocument getOrCreate(String itemId, String departmentName, String project, String location) {
        String d = blankToNull(departmentName), p = blankToNull(project), l = blankToNull(location);
        com.recnaile.inventory.masterdata.MasterDataItemResponse masterItem = null;
        if (masterDataItemClient != null) {
            masterItem = masterDataItemClient.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found in Master Data: " + itemId));
            if (masterItem.status() != null && !"ACTIVE".equalsIgnoreCase(masterItem.status())) {
                throw new IllegalStateException("Inactive Master Data item cannot receive or issue stock: " + itemId);
            }
            if (d == null) d = blankToNull(masterItem.department());
            if (l == null) l = blankToNull(masterItem.rack());
        }
        String key = key(itemId, d, p, l);
        final String finalDepartment = d;
        final String finalProject = p;
        final String finalLocation = l;
        final var finalMasterItem = masterItem;
        final int masterMinimum = finalMasterItem == null || finalMasterItem.minimumStock() == null
                ? 0 : finalMasterItem.minimumStock().intValue();
        final int masterMaximum = finalMasterItem == null || finalMasterItem.maximumStock() == null
                ? 0 : finalMasterItem.maximumStock().intValue();
        final int masterAvailable = finalMasterItem == null || finalMasterItem.availableStock() == null
                ? 0 : finalMasterItem.availableStock().intValueExact();
        final int masterDefect = finalMasterItem == null || finalMasterItem.defectStock() == null
                ? 0 : finalMasterItem.defectStock().intValueExact();
        return balanceRepository.findByBalanceKey(key).map(existing -> {
            // Master Data stock fields are opening-stock inputs only. If this
            // item has never produced an Inventory movement, hydrate an old
            // zero balance from Master Data before the first movement. After
            // that point Inventory movements are authoritative.
            if (movementRepository.findByItemId(itemId).isEmpty() &&
                    existing.getAvailableQuantity() == 0 && existing.getDefectQuantity() == 0) {
                existing.setAvailableQuantity(masterAvailable);
                existing.setDefectQuantity(masterDefect);
                recalculateStatus(existing);
                existing.setUpdatedAt(Instant.now());
            }
            return existing;
        }).orElseGet(() -> {
            StockBalanceDocument b = new StockBalanceDocument();
            b.setBalanceKey(key);
            b.setItemId(itemId);
            b.setDepartmentName(finalDepartment);
            b.setProjectId(finalProject);
            b.setLocation(finalLocation);
            b.setAvailableQuantity(masterAvailable);
            b.setDefectQuantity(masterDefect);
            b.setMinimumStock(masterMinimum);
            b.setMaximumStock(masterMaximum);
            b.setThresholdsOverridden(false);
            recalculateStatus(b);
            b.setUpdatedAt(Instant.now());
            return b;
        });
    }

    private Dimensions dimensions(JsonNode parent, JsonNode item) {
        String department = firstText(item, "departmentName", "departmentId");
        if (department == null) department = firstText(parent, "departmentName", "departmentId");
        String project = firstText(item, "projectId");
        if (project == null) project = firstText(parent, "projectId");
        String location = firstText(item, "location", "warehouseLocation");
        if (location == null) location = firstText(parent, "location", "warehouseLocation");
        return new Dimensions(department, project, location);
    }

    private record Dimensions(String departmentName, String projectId, String location) {
        String balanceKey(String itemId) {
            return InventoryEventService.key(itemId, departmentName, projectId, location);
        }
    }

    public static String key(String itemId, String dept, String project, String location) {
        return String.join("|", itemId, nullToBlank(dept), nullToBlank(project), nullToBlank(location));
    }

    private void recalculateStatus(StockBalanceDocument b) {
        boolean low = b.getAvailableQuantity() <= b.getMinimumStock();
        b.setLowStock(low);
        b.setStatus(b.getAvailableQuantity() <= 0 ? StockStatus.OUT_OF_STOCK
                : low ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK);
    }

    private String statusName(int available, int minimum) {
        if (available <= 0) return StockStatus.OUT_OF_STOCK.name();
        if (available <= minimum) return StockStatus.LOW_STOCK.name();
        return StockStatus.IN_STOCK.name();
    }

    private static JsonNode payload(JsonNode event) {
        JsonNode payload = event.get("payload");
        return payload != null && payload.isObject() ? payload : event;
    }

    private String resolveItemId(JsonNode node) {
        String itemId = text(node, "itemId");
        if (itemId != null && !itemId.isBlank()) return itemId.trim();
        String itemName = text(node, "itemName");
        if (itemName != null && !itemName.isBlank() && masterDataItemClient != null) {
            return masterDataItemClient.findUniqueByName(itemName.trim())
                    .map(item -> item.itemId())
                    .filter(id -> id != null && !id.isBlank())
                    .orElseThrow(() -> new IllegalArgumentException("No unique Master Data item found for itemName: " + itemName));
        }
        throw new IllegalArgumentException("itemId is required for Inventory stock events");
    }

    private static String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing payload field: " + field);
        return value;
    }

    private static String requiredSource(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing source reference in event");
        return value;
    }

    private static int firstInteger(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                if (!value.isNumber()) throw new IllegalArgumentException("Invalid integer payload field: " + field);
                try { return value.decimalValue().intValueExact(); }
                catch (ArithmeticException ex) { throw new IllegalArgumentException("Invalid integer payload field: " + field, ex); }
            }
        }
        throw new IllegalArgumentException("Missing integer payload field: " + fields[0]);
    }

    private static int integralQuantity(JsonNode value, String field) {
        if (value == null || value.isNull() || !value.isNumber()) {
            throw new IllegalArgumentException("Missing/invalid numeric payload field: " + field);
        }
        try {
            return value.decimalValue().intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Quantity must be a positive integer for Inventory stock: " + field, ex);
        }
    }

    private static void validateNonNegative(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " cannot be negative");
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? Instant.now() : Instant.parse(value);
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private static String nullToBlank(String value) { return value == null ? "" : value; }
}
