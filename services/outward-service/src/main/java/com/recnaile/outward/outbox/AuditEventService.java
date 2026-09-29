package com.recnaile.outward.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.slf4j.MDC;

/** Creates reliable append-only audit events through the transactional outbox. */
@Service
public class AuditEventService {

    public static final String AUDIT_EVENT = "AUDIT_EVENT";
    public static final int EVENT_VERSION = 1;
    public static final String SOURCE_SERVICE = "outward-service";

    private final TransactionalOutboxService outboxService;
    private final Clock clock;

    @Autowired
    public AuditEventService(TransactionalOutboxService outboxService) {
        this(outboxService, Clock.systemUTC());
    }

    AuditEventService(TransactionalOutboxService outboxService, Clock clock) {
        this.outboxService = outboxService;
        this.clock = clock;
    }

    public OutboxEvent record(String entityType, String entityId, String action,
                              Authentication authentication, String beforeState,
                              String afterState, String reason) {
        String actorId = authentication == null ? "system" : authentication.getName();
        String actorRole = resolveRole(authentication);
        Instant occurredAt = Instant.now(clock);
        String correlationId = correlationId();

        AuditEventPayload payload = new AuditEventPayload(
                UUID.randomUUID().toString(),
                AUDIT_EVENT,
                EVENT_VERSION,
                occurredAt,
                entityType,
                entityId,
                action,
                actorId,
                actorRole,
                correlationId,
                SOURCE_SERVICE,
                beforeState,
                afterState,
                reason);

        return outboxService.enqueueAudit(payload);
    }

    private String resolveRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a != null && !a.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String correlationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = MDC.get("traceId");
        }
        return correlationId;
    }
}
