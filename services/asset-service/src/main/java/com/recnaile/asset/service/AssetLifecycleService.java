package com.recnaile.asset.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final com.recnaile.asset.event.AssetEventPublisher eventPublisher;
    private final com.recnaile.asset.event.AssetAuditEventPublisher auditEventPublisher;

    @Autowired
    public AssetLifecycleService(
            AssetRepository assetRepository,
            com.recnaile.asset.event.AssetEventPublisher eventPublisher,
            com.recnaile.asset.event.AssetAuditEventPublisher auditEventPublisher) {
        this.assetRepository = assetRepository;
        this.eventPublisher = eventPublisher;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Test-friendly constructor retained for lifecycle unit tests. */
    AssetLifecycleService(AssetRepository assetRepository) {
        this(assetRepository, null, null);
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
        String previousStatus = asset.getStatus().name();
        asset.completeRepair(now);
        Asset saved = assetRepository.save(asset);
        if (eventPublisher != null) eventPublisher.publishStatusChanged(saved, previousStatus, saved.getStatus().name());
        if (auditEventPublisher != null) auditEventPublisher.publish("ASSET_STATUS_CHANGED", saved.getAssetId(), previousStatus, saved.getStatus(), null);
        return saved;
    }

    @Transactional
    public Asset retire(String assetId, Instant now) {
        Asset asset = findAsset(assetId);
        String previousStatus = asset.getStatus().name();
        asset.retire(now);
        Asset saved = assetRepository.save(asset);
        if (eventPublisher != null) eventPublisher.publishStatusChanged(saved, previousStatus, saved.getStatus().name());
        if (auditEventPublisher != null) auditEventPublisher.publish("ASSET_RETIRED", saved.getAssetId(), previousStatus, saved.getStatus(), null);
        return saved;
    }

    private Asset findAsset(String assetId) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        return assetRepository.findById(normalizedAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + normalizedAssetId));
    }
}
