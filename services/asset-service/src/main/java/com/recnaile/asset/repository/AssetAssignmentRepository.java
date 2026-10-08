package com.recnaile.asset.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.asset.model.AssetAssignment;

public interface AssetAssignmentRepository extends MongoRepository<AssetAssignment, String> {

    boolean existsByAssetIdAndReturnedAtIsNull(String assetId);

    Optional<AssetAssignment> findById(String assignmentId);

    Optional<AssetAssignment> findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc(String assetId);

    List<AssetAssignment> findByAssetIdOrderByAssignedAtDesc(String assetId);

    List<AssetAssignment> findByOutwardNumberAndReturnedAtIsNullOrderByAssignedAtAsc(String outwardNumber);

    List<AssetAssignment> findByEmployeeIdOrderByAssignedAtDesc(String employeeId);

    List<AssetAssignment> findByDepartmentNameOrderByAssignedAtDesc(String departmentName);
}
