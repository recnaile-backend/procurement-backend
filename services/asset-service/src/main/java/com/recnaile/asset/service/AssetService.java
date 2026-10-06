package com.recnaile.asset.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.exception.ResourceNotFoundException;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.repository.AssetRepository;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetIdGenerator assetIdGenerator;

    public AssetService(AssetRepository assetRepository, AssetIdGenerator assetIdGenerator) {
        this.assetRepository = assetRepository;
        this.assetIdGenerator = assetIdGenerator;
    }

    @Transactional
    public Asset create(
            String itemId,
            String issueReference,
            String name,
            String brand,
            String model,
            String issuedBy,
            AssetCondition condition,
            String remarks,
            Instant now) {
        requireText(itemId, "Item ID");
        requireText(issueReference, "Issue reference");
        if (assetRepository.findByIssueReference(issueReference).isPresent()) {
            throw new IllegalStateException("Asset already exists for issue reference: " + issueReference);
        }
        return assetRepository.save(Asset.create(
                assetIdGenerator.next(), itemId, issueReference, name, brand, model,
                issuedBy, AssetStatus.RETURNED, condition, remarks, now));
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

    public Asset getByIssueReference(String issueReference) {
        requireText(issueReference, "Issue reference");
        return assetRepository.findByIssueReference(issueReference)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found for issue reference: " + issueReference));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
