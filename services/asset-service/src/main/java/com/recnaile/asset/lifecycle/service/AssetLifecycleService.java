package com.recnaile.asset.lifecycle.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.asset.assignment.model.AssetAssignment;
import com.recnaile.asset.assignment.repository.AssetAssignmentRepository;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.repository.AssetRepository;

/** Coordinates lifecycle state transitions and closes assignment history when an asset is returned. */
@Service
public class AssetLifecycleService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;

    public AssetLifecycleService(AssetRepository assetRepository,
                                 AssetAssignmentRepository assignmentRepository) {
        this.assetRepository = assetRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public Asset returnAsset(String assetId, AssetCondition condition, Instant now) {
        Asset asset = getAsset(assetId);
        AssetAssignment assignment = assignmentRepository.findFirstByAssetIdAndActiveTrue(asset.getAssetId())
                .orElseThrow(() -> new IllegalStateException("No active assignment for asset: " + assetId));
        asset.markReturned(condition, now);
        assignment.close(now);
        assetRepository.save(asset);
        assignmentRepository.save(assignment);
        return asset;
    }

    @Transactional
    public Asset completeRepair(String assetId, Instant now) {
        Asset asset = getAsset(assetId);
        asset.completeRepair(now);
        return assetRepository.save(asset);
    }

    @Transactional
    public Asset retire(String assetId, Instant now) {
        Asset asset = getAsset(assetId);
        asset.retire(now);
        return assetRepository.save(asset);
    }

    private Asset getAsset(String assetId) {
        return assetRepository.findById(AssetId.of(assetId).value())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
    }
}
