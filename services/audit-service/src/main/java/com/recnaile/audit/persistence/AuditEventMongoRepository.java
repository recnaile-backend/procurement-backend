package com.recnaile.audit.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

/** Mongo persistence access. The event UUID is the Mongo _id and is inherently unique. */
public interface AuditEventMongoRepository extends MongoRepository<AuditEventDocument, UUID> {
    List<AuditEventDocument> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    List<AuditEventDocument> findByActorIdOrderByTimestampDesc(String actorId);
    List<AuditEventDocument> findByCorrelationIdOrderByTimestampDesc(String correlationId);
    List<AuditEventDocument> findByTraceIdOrderByTimestampDesc(String traceId);
    List<AuditEventDocument> findByServiceAndActionOrderByTimestampDesc(String service, String action);
}
