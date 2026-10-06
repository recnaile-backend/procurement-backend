package com.recnaile.asset.assignment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.asset.assignment.model.AssetAssignment;

public interface AssetAssignmentRepository extends MongoRepository<AssetAssignment, String> {

    Optional<AssetAssignment> findFirstByAssetIdAndActiveTrue(String assetId);

    List<AssetAssignment> findByAssetIdOrderByAssignedAtDesc(String assetId);

    List<AssetAssignment> findByAssignedUserIdOrderByAssignedAtDesc(String assignedUserId);

    List<AssetAssignment> findByDepartmentNameOrderByAssignedAtDesc(String departmentName);
}
