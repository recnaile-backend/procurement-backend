package com.recnaile.masterdata.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.masterdata.model.MasterDataStatus;
import com.recnaile.masterdata.model.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {

    Optional<Project> findByProjectName(String projectName);

    boolean existsByProjectName(String projectName);

    boolean existsByProjectIdAndStatus(String projectId, MasterDataStatus status);
}
