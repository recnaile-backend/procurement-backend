package com.recnaile.audit.application;

import com.recnaile.audit.persistence.AuditEventMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Reports retention candidates only; audit records are never deleted automatically by this service. */
@Service
public class AuditRetentionService {
    private final AuditEventMongoRepository repository;
    private final Clock clock;
    private final int retentionDays;

    @Autowired
    public AuditRetentionService(AuditEventMongoRepository repository,
            @Value("${app.audit.retention-days:0}") int retentionDays) {
        this(repository, retentionDays, Clock.systemUTC());
    }

    AuditRetentionService(AuditEventMongoRepository repository, int retentionDays, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (retentionDays < 0) throw new IllegalArgumentException("retentionDays cannot be negative");
        this.retentionDays = retentionDays;
    }

    public RetentionPreview preview() {
        if (retentionDays == 0) return new RetentionPreview(false, 0, null, 0,
                "Retention is disabled; no records are eligible for automated retention handling.");
        Instant cutoff = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
        return new RetentionPreview(true, retentionDays, cutoff, repository.countByTimestampBefore(cutoff),
                "Preview only. No audit records were deleted or modified.");
    }

    public record RetentionPreview(boolean enabled, int retentionDays, Instant cutoff, long candidateCount, String message) { }
}
