package com.recnaile.masterdata.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.masterdata.dto.item.CreateItemRequest;
import com.recnaile.masterdata.dto.item.ItemResponse;
import com.recnaile.masterdata.dto.item.UpdateItemRequest;
import com.recnaile.masterdata.model.Item;
import com.recnaile.masterdata.model.MasterDataStatus;
import com.recnaile.masterdata.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ItemResponse create(CreateItemRequest request) {

        String itemId = normalizeValue(request.itemId());

        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException(
                    "Item ID is required");
        }

        if (itemRepository.existsById(itemId)) {
            throw new IllegalArgumentException(
                    "Item ID already exists");
        }

        String barcode = normalizeValue(
                request.barcodeQrIdentifier());


        validateStockRange(
                request.minimumStock(),
                request.maximumStock());

        Instant now = Instant.now();

        Item item = Item.builder()
                .itemId(itemId)
                .name(normalizeValue(request.name()))
                .category(normalizeValue(request.category()))
                .unitOfMeasure(
                        normalizeValue(request.unitOfMeasure()))
                .type(request.type())
                .department(
                        normalizeValue(request.department()))
                .assetTracked(request.assetTracked())
                .defaultUnitPrice(request.defaultUnitPrice())
                .gstRate(request.gstRate())
                .barcodeQrIdentifier(barcode)
                .minimumStock(request.minimumStock())
                .maximumStock(request.maximumStock())
                .availableStock(request.availableStock())
                .defectStock(request.defectStock())
                .rack(normalizeValue(request.rack()))
                .remark(normalizeValue(request.remark()))
                .status(MasterDataStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Item saved = itemRepository.save(item);

        return toResponse(saved);
    }

    @Transactional
    public List<ItemResponse> multiAdd(List<CreateItemRequest> requests) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("At least one item is required");
        java.util.Set<String> ids = new java.util.HashSet<>();
        Instant now = Instant.now();
        List<Item> items = requests.stream().map(request -> {
            String itemId = normalizeValue(request.itemId());
            if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("Item ID is required");
            if (!ids.add(itemId)) throw new IllegalArgumentException("Duplicate Item ID in request: " + itemId);
            if (itemRepository.existsById(itemId)) throw new IllegalArgumentException("Item ID already exists: " + itemId);
            validateStockRange(request.minimumStock(), request.maximumStock());
            return Item.builder().itemId(itemId).name(normalizeValue(request.name())).category(normalizeValue(request.category())).unitOfMeasure(normalizeValue(request.unitOfMeasure())).type(request.type()).department(normalizeValue(request.department())).assetTracked(request.assetTracked()).defaultUnitPrice(request.defaultUnitPrice()).gstRate(request.gstRate()).barcodeQrIdentifier(normalizeValue(request.barcodeQrIdentifier())).minimumStock(request.minimumStock()).maximumStock(request.maximumStock()).availableStock(request.availableStock()).defectStock(request.defectStock()).rack(normalizeValue(request.rack())).remark(normalizeValue(request.remark())).status(MasterDataStatus.ACTIVE).createdAt(now).updatedAt(now).build();
        }).toList();
        return itemRepository.saveAll(items).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ItemResponse update(
            String itemId,
            UpdateItemRequest request) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"));

        if (item.getStatus() == MasterDataStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Inactive item cannot be updated");
        }

        if (request.version() == null) {
            throw new IllegalArgumentException(
                    "Item version is required for update");
        }

        if (!request.version().equals(item.getVersion())) {
            throw new OptimisticLockingFailureException(
                    "Item version conflict");
        }

        String barcode = normalizeValue(
                request.barcodeQrIdentifier());

        validateStockRange(
                request.minimumStock(),
                request.maximumStock());

        item.setName(
                normalizeValue(request.name()));

        item.setCategory(
                normalizeValue(request.category()));

        item.setUnitOfMeasure(
                normalizeValue(request.unitOfMeasure()));

        item.setType(request.type());

        item.setDepartment(
                normalizeValue(request.department()));

        item.setAssetTracked(
                request.assetTracked());

        item.setDefaultUnitPrice(
                request.defaultUnitPrice());

        item.setGstRate(
                request.gstRate());

        item.setBarcodeQrIdentifier(barcode);

        item.setMinimumStock(
                request.minimumStock());

        item.setMaximumStock(
                request.maximumStock());

        item.setAvailableStock(
                request.availableStock());

        item.setDefectStock(
                request.defectStock());

        item.setRack(
                normalizeValue(request.rack()));

        item.setRemark(
                normalizeValue(request.remark()));

        item.setUpdatedAt(Instant.now());

        Item saved = itemRepository.save(item);

        return toResponse(saved);
    }

    @Transactional
    public ItemResponse deactivate(String itemId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"));

        if (item.getStatus() == MasterDataStatus.INACTIVE) {
            return toResponse(item);
        }

        item.setStatus(MasterDataStatus.INACTIVE);
        item.setUpdatedAt(Instant.now());

        Item saved = itemRepository.save(item);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ItemResponse getById(String itemId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"));

        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getAll() {

        return itemRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String itemId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found"));

        itemRepository.delete(item);
    }

    private ItemResponse toResponse(Item item) {

        BigDecimal totalPrice =
                calculateTotalPrice(item);

        return new ItemResponse(
                item.getItemId(),
                item.getName(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getDepartment(),
                item.getMinimumStock(),
                item.getMaximumStock(),
                item.getAvailableStock(),
                item.getDefectStock(),
                item.getType(),
                item.getUnitOfMeasure(),
                item.getDefaultUnitPrice(),
                item.getGstRate(),
                totalPrice,
                item.getRack(),
                item.getStatus(),
                item.getRemark(),
                item.getCategory(),
                item.isAssetTracked(),
                item.getBarcodeQrIdentifier(),
                item.getVersion()
        );
    }

    private BigDecimal calculateTotalPrice(Item item) {

        if (item.getAvailableStock() == null
                || item.getDefaultUnitPrice() == null) {

            return null;
        }

        return item.getAvailableStock()
                .multiply(item.getDefaultUnitPrice());
    }

    private void validateStockRange(
            BigDecimal minimumStock,
            BigDecimal maximumStock) {

        if (minimumStock != null
                && maximumStock != null
                && minimumStock.compareTo(maximumStock) > 0) {

            throw new IllegalArgumentException(
                    "Minimum stock cannot be greater than maximum stock");
        }
    }

    private String normalizeValue(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}