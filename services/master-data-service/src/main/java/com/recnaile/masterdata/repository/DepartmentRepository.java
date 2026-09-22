package com.recnaile.masterdata.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.masterdata.model.Department;
import com.recnaile.masterdata.model.MasterDataStatus;

public interface DepartmentRepository extends MongoRepository<Department, String> {

    Optional<Department> findByDepartmentName(String departmentName);

    boolean existsByDepartmentName(String departmentName);

    boolean existsByIdAndStatus(String id, MasterDataStatus status);
}
