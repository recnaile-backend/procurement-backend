package com.recnaile.asset.assignment.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.asset.assignment.model.AssetAssignment;
import com.recnaile.asset.assignment.repository.AssetAssignmentRepository;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.repository.AssetRepository;

/** Coordinates the current asset assignment snapshot and immutable history. */
@Service
public class AssetAssignmentService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;

    public AssetAssignmentService(
            AssetRepository assetRepository,
            AssetAssignmentRepository assignmentRepository) {
        this.assetRepository = assetRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public AssetAssignment assign(
            String assetId,
            String assignedUserId,
            String departmentName,
            String assignedBy,
            String remarks,
            Instant now) {
        Asset asset = getAsset(assetId);
        if (assignmentRepository.findFirstByAssetIdAndActiveTrue(assetId).isPresent()) {
            throw new IllegalStateException("Asset already has an active assignment: " + assetId);
        }

        AssetAssignment assignment = AssetAssignment.create(
                asset.getAssetId(),
                asset.getItemId(),
                assignedUserId,
                departmentName,
                assignedBy,
                now,
                remarks);

        asset.assignTo(assignedUserId, departmentName, now);
        assetRepository.save(asset);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public AssetAssignment closeCurrentAssignment(String assetId, Instant now) {
        AssetAssignment assignment = assignmentRepository.findFirstByAssetIdAndActiveTrue(assetId)
                .orElseThrow(() -> new IllegalStateException("No active assignment for asset: " + assetId));
        assignment.close(now);

        Asset asset = getAsset(assetId);
        asset.markReturned(com.recnaile.asset.model.AssetCondition.GOOD, now);
        assetRepository.save(asset);
        return assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssetAssignment> history(String assetId) {
        return assignmentRepository.findByAssetIdOrderByAssignedAtDesc(assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetAssignment> employeeHistory(String assignedUserId) {
        return assignmentRepository.findByAssignedUserIdOrderByAssignedAtDesc(assignedUserId);
    }

    @Transactional(readOnly = true)
    public List<AssetAssignment> departmentHistory(String departmentName) {
        return assignmentRepository.findByDepartmentNameOrderByAssignedAtDesc(departmentName);
    }

    private Asset getAsset(String assetId) {
        return assetRepository.findById(AssetId.of(assetId).value())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
    }
}
