package com.recnaile.reporting.repository;

import com.recnaile.reporting.model.ReportingProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ReportingProjectionRepository extends MongoRepository<ReportingProjection, String> {
    Optional<ReportingProjection> findByEntityTypeAndEntityId(String entityType, String entityId);
    long countByEntityType(String entityType);
    long countByEntityTypeAndStatus(String entityType, String status);
}
