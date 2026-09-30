package com.recnaile.outward.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Creates reliable notification hooks through the transactional outbox. */
@Service
public class NotificationEventService {

    public static final String APPROVAL = TransactionalOutboxService.OUTWARD_APPROVED;
    public static final String REJECTION = TransactionalOutboxService.OUTWARD_REJECTED;
    private static final int EVENT_VERSION = 1;
    private static final String SOURCE_SERVICE = "outward-service";

    private final TransactionalOutboxService outboxService;
    private final Clock clock;

    @Autowired
    public NotificationEventService(TransactionalOutboxService outboxService) {
        this(outboxService, Clock.systemUTC());
    }

    NotificationEventService(TransactionalOutboxService outboxService, Clock clock) {
        this.outboxService = outboxService;
        this.clock = clock;
    }

    public OutboxEvent notifyApproval(String outwardNumber, String requesterId,
                                      Authentication authentication) {
        return create(outwardNumber, requesterId, APPROVAL, "Outward request approved",
                "Your outward request " + outwardNumber + " has been approved.", authentication);
    }

    public OutboxEvent notifyRejection(String outwardNumber, String requesterId, String reason,
                                       Authentication authentication) {
        String message = "Your outward request " + outwardNumber + " has been rejected.";
        if (reason != null && !reason.isBlank()) {
            message += " Reason: " + reason;
        }
        return create(outwardNumber, requesterId, REJECTION, "Outward request rejected",
                message, authentication);
    }

    private OutboxEvent create(String outwardNumber, String requesterId, String type,
                               String title, String message, Authentication authentication) {
        Instant occurredAt = Instant.now(clock);
        String actorId = authentication == null ? "system" : authentication.getName();
        String role = authentication == null || authentication.getAuthorities() == null ? null :
                authentication.getAuthorities().stream().map(a -> a.getAuthority())
                        .filter(a -> a != null && !a.isBlank()).findFirst().orElse(null);
        NotificationEventPayload payload = new NotificationEventPayload(
                UUID.randomUUID().toString(),
                type,
                EVENT_VERSION,
                occurredAt,
                type,
                requesterId,
                null,
                "OUTWARD_REQUEST",
                outwardNumber,
                type,
                title,
                message,
                correlationId(),
                SOURCE_SERVICE);
        return outboxService.enqueueNotification(payload);
    }

    private String correlationId() {
        String value = MDC.get("correlationId");
        return value == null || value.isBlank() ? MDC.get("traceId") : value;
    }
}
