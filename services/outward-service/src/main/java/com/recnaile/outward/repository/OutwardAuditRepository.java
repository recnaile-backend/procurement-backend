package com.recnaile.outward.repository;

import java.util.List;

import com.recnaile.outward.domain.model.OutwardAuditRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutwardAuditRepository extends MongoRepository<OutwardAuditRecord, String> {
    List<OutwardAuditRecord> findByEntityIdOrderByOccurredAtDesc(String entityId);
}
