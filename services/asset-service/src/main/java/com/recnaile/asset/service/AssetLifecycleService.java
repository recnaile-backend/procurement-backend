package com.recnaile.asset.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.exception.ResourceNotFoundException;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.repository.AssetRepository;

/**
 * Owns the Asset lifecycle state machine. Stock quantities are deliberately
 * outside this service; this service changes only the asset lifecycle state.
 */
@Service
public class AssetLifecycleService {

    private final AssetRepository assetRepository;

    public AssetLifecycleService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Transactional
    public Asset markReturned(String assetId, AssetCondition condition, Instant now) {
        Asset asset = findAsset(assetId);
        asset.returnFromAssignment(condition, now);
        return assetRepository.save(asset);
    }

    @Transactional
    public Asset completeRepair(String assetId, Instant now) {
        Asset asset = findAsset(assetId);
        asset.completeRepair(now);
        return assetRepository.save(asset);
    }

    @Transactional
    public Asset retire(String assetId, Instant now) {
        Asset asset = findAsset(assetId);
        asset.retire(now);
        return assetRepository.save(asset);
    }

    private Asset findAsset(String assetId) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        return assetRepository.findById(normalizedAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + normalizedAssetId));
    }
}
