package com.recnaile.asset.service;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.exception.ResourceNotFoundException;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.api.dto.AssetGroupResponse;
import com.recnaile.asset.repository.AssetRepository;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetIdGenerator assetIdGenerator;
    private final com.recnaile.asset.event.AssetAuditEventPublisher auditEventPublisher;

    @Autowired
    public AssetService(
            AssetRepository assetRepository,
            AssetIdGenerator assetIdGenerator,
            com.recnaile.asset.event.AssetAuditEventPublisher auditEventPublisher) {
        this.assetRepository = assetRepository;
        this.assetIdGenerator = assetIdGenerator;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Test-friendly constructor retained for service unit tests that do not exercise Kafka audit publication. */
    AssetService(AssetRepository assetRepository, AssetIdGenerator assetIdGenerator) {
        this(assetRepository, assetIdGenerator, null);
    }

    @Transactional
    public Asset create(
            String itemId,
            String outwardNumber,
            String name,
            String brand,
            String model,
            String issuedBy,
            AssetCondition condition,
            String remarks,
            Instant now) {
        requireText(itemId, "Item ID");
        String normalizedOutwardNumber = ReferenceNumberNormalizer.outwardNumber(outwardNumber);
        Asset saved = assetRepository.save(Asset.create(
                assetIdGenerator.next(), itemId, normalizedOutwardNumber, name, brand, model,
                issuedBy, AssetStatus.RETURNED, condition, remarks, now));
        if (auditEventPublisher != null) {
            auditEventPublisher.publish("ASSET_CREATED", saved.getAssetId(), null, saved, issuedBy);
        }
        return saved;
    }

    public Asset get(String assetId) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        return assetRepository.findById(normalizedAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + normalizedAssetId));
    }

    public List<Asset> getAll(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return assetRepository.findAll();
        }
        return List.of(get(assetId));
    }

    public List<Asset> getByOutwardNumber(String outwardNumber) {
        String normalizedOutwardNumber = ReferenceNumberNormalizer.outwardNumber(outwardNumber);
        List<Asset> assets = assetRepository.findByOutwardNumberOrderByAssetIdAsc(normalizedOutwardNumber);
        if (assets.isEmpty()) {
            throw new ResourceNotFoundException("Assets not found for outward number: " + normalizedOutwardNumber);
        }
        return assets;
    }

    public List<Asset> getByOutwardNumberAndItem(String outwardNumber, String itemId) {
        String normalizedOutwardNumber = ReferenceNumberNormalizer.outwardNumber(outwardNumber);
        requireText(itemId, "Item ID");
        return assetRepository.findByOutwardNumberAndItemIdOrderByAssetIdAsc(normalizedOutwardNumber, itemId);
    }

    /**
     * Builds a compact read-only group view from the individual asset records.
     * A group is uniquely identified by outward number + item ID. No separate
     * AssetGroup collection is persisted, so the physical asset records remain
     * the single source of truth.
     */
    public List<AssetGroupResponse> getGroups() {
        return toGroups(assetRepository.findAll());
    }

    public List<AssetGroupResponse> toGroups(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return List.of();
        }
        Map<String, List<Asset>> groups = assets.stream()
                .collect(Collectors.groupingBy(
                        asset -> asset.getOutwardNumber() + "\u0000" + asset.getItemId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return groups.values().stream()
                .map(groupAssets -> {
                    Asset first = groupAssets.get(0);
                    return new AssetGroupResponse(
                            first.getOutwardNumber(),
                            first.getItemId(),
                            groupAssets.size(),
                            count(groupAssets, AssetStatus.IN_USE),
                            count(groupAssets, AssetStatus.RETURNED),
                            count(groupAssets, AssetStatus.UNDER_REPAIR),
                            count(groupAssets, AssetStatus.RETIRED),
                            groupAssets.stream()
                                    .map(Asset::getAssignedUserId)
                                    .filter(value -> value != null && !value.isBlank())
                                    .distinct()
                                    .sorted()
                                    .toList());
                })
                .toList();
    }

    private static int count(List<Asset> assets, AssetStatus status) {
        return (int) assets.stream().filter(asset -> asset.getStatus() == status).count();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
