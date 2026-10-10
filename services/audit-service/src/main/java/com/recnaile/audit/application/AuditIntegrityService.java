package com.recnaile.audit.application;

import com.recnaile.audit.persistence.AuditEventDocument;
import com.recnaile.audit.persistence.AuditEventIntegrity;
import com.recnaile.audit.persistence.AuditEventMongoRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditIntegrityService {
    private final AuditEventMongoRepository repository;
    public AuditIntegrityService(AuditEventMongoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }
    public Optional<IntegrityReport> verify(UUID eventId) {
        return repository.findById(eventId).map(event -> {
            boolean hasHash = event.integrityHash() != null && !event.integrityHash().isBlank();
            return new IntegrityReport(event.eventId(), hasHash ? (AuditEventIntegrity.isValid(event) ? "VALID" : "MISMATCH") : "UNVERIFIABLE_LEGACY_RECORD",
                    hasHash, "Verification compares the stored SHA-256 digest with the current persisted fields.");
        });
    }
    public record IntegrityReport(UUID eventId, String status, boolean digestPresent, String details) { }
}
