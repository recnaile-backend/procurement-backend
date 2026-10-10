package com.recnaile.audit.application;

import com.recnaile.audit.persistence.AuditEventDocument;
import com.recnaile.audit.persistence.AuditEventMongoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Read-only application queries for audit history. No mutation methods are exposed. */
@Service
public class AuditEventQueryService {
    private final AuditEventMongoRepository repository;

    public AuditEventQueryService(AuditEventMongoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<AuditEventDocument> findByEventId(UUID eventId) {
        if (eventId == null) throw new IllegalArgumentException("Event ID is required");
        return repository.findById(eventId);
    }

    public List<AuditEventDocument> findEntityHistory(String entityType, String entityId) {
        requireText(entityType, "Entity type");
        requireText(entityId, "Entity ID");
        return repository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    public List<AuditEventDocument> findActorHistory(String actorId) {
        requireText(actorId, "Actor ID");
        return repository.findByActorIdOrderByTimestampDesc(actorId);
    }

    public List<AuditEventDocument> findCorrelationHistory(String correlationId) {
        requireText(correlationId, "Correlation ID");
        return repository.findByCorrelationIdOrderByTimestampDesc(correlationId);
    }

    public List<AuditEventDocument> findTraceHistory(String traceId) {
        requireText(traceId, "Trace ID");
        return repository.findByTraceIdOrderByTimestampDesc(traceId);
    }

    public List<AuditEventDocument> findServiceActionHistory(String service, String action) {
        requireText(service, "Service");
        requireText(action, "Action");
        return repository.findByServiceAndActionOrderByTimestampDesc(service, action);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
