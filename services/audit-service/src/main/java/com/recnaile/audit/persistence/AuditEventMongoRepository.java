package com.recnaile.audit.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

/** Mongo persistence access. The event UUID is the Mongo _id and is inherently unique. */
public interface AuditEventMongoRepository extends MongoRepository<AuditEventDocument, UUID> {
    List<AuditEventDocument> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    Page<AuditEventDocument> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
    List<AuditEventDocument> findByActorIdOrderByTimestampDesc(String actorId);
    Page<AuditEventDocument> findByActorId(String actorId, Pageable pageable);
    List<AuditEventDocument> findByCorrelationIdOrderByTimestampDesc(String correlationId);
    Page<AuditEventDocument> findByCorrelationId(String correlationId, Pageable pageable);
    List<AuditEventDocument> findByTraceIdOrderByTimestampDesc(String traceId);
    Page<AuditEventDocument> findByTraceId(String traceId, Pageable pageable);
    List<AuditEventDocument> findByServiceAndActionOrderByTimestampDesc(String service, String action);
    Page<AuditEventDocument> findByServiceAndAction(String service, String action, Pageable pageable);
    long countByTimestampBefore(Instant cutoff);
}
