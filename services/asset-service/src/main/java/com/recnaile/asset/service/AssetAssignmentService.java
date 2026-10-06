package com.recnaile.asset.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.exception.ResourceNotFoundException;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.repository.AssetAssignmentRepository;
import com.recnaile.asset.repository.AssetRepository;

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

    /** Starts one active assignment while preserving all previous assignments. */
    @Transactional
    public AssetAssignment startAssignment(
            String assetId,
            String employeeId,
            String departmentName,
            String issuedBy,
            String outwardId,
            Instant assignedAt,
            String remarks) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        Asset asset = findAsset(normalizedAssetId);

        if (assignmentRepository.existsByAssetIdAndReturnedAtIsNull(normalizedAssetId)
                || asset.getAssignedUserId() != null) {
            throw new IllegalStateException("Asset already has an active assignment");
        }

        if (asset.getStatus() == com.recnaile.asset.model.AssetStatus.RETURNED) {
            asset.beginNewLifecycle(employeeId, departmentName, assignedAt);
        } else {
            asset.assignTo(employeeId, departmentName, assignedAt);
        }
        AssetAssignment assignment = AssetAssignment.start(
                normalizedAssetId,
                employeeId,
                departmentName,
                issuedBy,
                outwardId,
                assignedAt,
                remarks);

        assetRepository.save(asset);
        return assignmentRepository.save(assignment);
    }

    /** Closes only the active assignment; the historical record remains stored. */
    @Transactional
    public AssetAssignment closeAssignment(
            String assetId,
            Instant returnedAt,
            String returnId,
            AssetCondition returnCondition) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        Asset asset = findAsset(normalizedAssetId);
        AssetAssignment assignment = assignmentRepository
                .findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc(normalizedAssetId)
                .orElseThrow(() -> new IllegalStateException("No active assignment exists for asset: " + normalizedAssetId));

        asset.returnFromAssignment(returnCondition, returnedAt);
        assignment.close(returnedAt, returnId, returnCondition);
        assetRepository.save(asset);
        return assignmentRepository.save(assignment);
    }

    public AssetAssignment getById(String assignmentId) {
        requireText(assignmentId, "Assignment ID");
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
    }

    public List<AssetAssignment> historyForAsset(String assetId) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        return assignmentRepository.findByAssetIdOrderByAssignedAtDesc(normalizedAssetId);
    }

    public List<AssetAssignment> historyForEmployee(String employeeId) {
        requireText(employeeId, "Employee ID");
        return assignmentRepository.findByEmployeeIdOrderByAssignedAtDesc(employeeId);
    }

    public List<AssetAssignment> historyForDepartment(String departmentName) {
        requireText(departmentName, "Department name");
        return assignmentRepository.findByDepartmentNameOrderByAssignedAtDesc(departmentName);
    }

    private Asset findAsset(String assetId) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        return assetRepository.findById(normalizedAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + normalizedAssetId));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
