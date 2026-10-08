package com.recnaile.asset.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final com.recnaile.asset.event.AssetEventPublisher eventPublisher;
    private final com.recnaile.asset.event.AssetAuditEventPublisher auditEventPublisher;

    @Autowired
    public AssetAssignmentService(
            AssetRepository assetRepository,
            AssetAssignmentRepository assignmentRepository,
            com.recnaile.asset.event.AssetEventPublisher eventPublisher,
            com.recnaile.asset.event.AssetAuditEventPublisher auditEventPublisher) {
        this.assetRepository = assetRepository;
        this.assignmentRepository = assignmentRepository;
        this.eventPublisher = eventPublisher;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Test-friendly constructor retained for unit tests that do not exercise Kafka publication. */
    AssetAssignmentService(
            AssetRepository assetRepository,
            AssetAssignmentRepository assignmentRepository) {
        this(assetRepository, assignmentRepository, null, null);
    }

    /** Starts one active assignment while preserving all previous assignments. */
    @Transactional
    public AssetAssignment startAssignment(
            String assetId,
            String employeeId,
            String departmentName,
            String issuedBy,
            String outwardNumber,
            Instant assignedAt,
            String remarks) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        final String normalizedOutwardNumber = ReferenceNumberNormalizer.outwardNumber(outwardNumber);
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
                normalizedOutwardNumber,
                assignedAt,
                remarks);

        Asset savedAsset = assetRepository.save(asset);
        AssetAssignment savedAssignment = assignmentRepository.save(assignment);
        if (eventPublisher != null) eventPublisher.publishAssetAssigned(savedAsset, savedAssignment);
        if (auditEventPublisher != null) {
            auditEventPublisher.publish("ASSET_ASSIGNED", savedAsset.getAssetId(), null, savedAssignment, savedAssignment.getIssuedBy());
        }
        return savedAssignment;
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
        assignment.close(returnedAt, ReferenceNumberNormalizer.returnId(returnId), returnCondition);
        Asset savedAsset = assetRepository.save(asset);
        AssetAssignment savedAssignment = assignmentRepository.save(assignment);
        if (eventPublisher != null) eventPublisher.publishAssetReturned(savedAsset, savedAssignment);
        if (auditEventPublisher != null) {
            auditEventPublisher.publish("ASSET_RETURNED", savedAsset.getAssetId(), null, savedAssignment, savedAssignment.getReturnId());
        }
        return savedAssignment;
    }

    public AssetAssignment getActiveAssignment(String assetId) {
        final String normalizedAssetId = AssetId.normalize(assetId);
        return assignmentRepository
                .findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc(normalizedAssetId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active assignment exists for asset: " + normalizedAssetId));
    }

    public AssetAssignment getById(String assignmentId) {
        requireText(assignmentId, "Assignment ID");
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
    }

    public List<AssetAssignment> activeAssignmentsForOutward(String outwardNumber) {
        String normalizedOutwardNumber = ReferenceNumberNormalizer.outwardNumber(outwardNumber);
        return assignmentRepository.findByOutwardNumberAndReturnedAtIsNullOrderByAssignedAtAsc(normalizedOutwardNumber);
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
