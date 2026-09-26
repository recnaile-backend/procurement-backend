package com.recnaile.inward.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.model.OutboxEvent;
import com.recnaile.inward.repository.OutboxEventRepository;
import com.recnaile.inward.observability.InwardMetrics;

/**
 * Creates durable business events in the transactional outbox.
 *
 * Phase 12 publishes one GOODS_RECEIVED event per GRN line so downstream
 * consumers can update stock at item granularity.
 */
@Service
public class OutboxService {

    private static final String EVENT_TYPE = "GOODS_RECEIVED";
    private static final String AUDIT_EVENT_TYPE = "AUDIT_EVENT";
    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "inward-service";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String topic;
    private final InwardMetrics metrics;

    @Autowired
    public OutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.outbox.topic:procurement.events}") String topic) {
        this(repository, objectMapper, clock, topic, null);
    }

    public OutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            Clock clock,
            String topic,
            InwardMetrics metrics) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.topic = topic;
        this.metrics = metrics;
    }

    public void enqueueGoodsReceived(GRNResponse grn) {
        if (grn.items() == null || grn.items().isEmpty()) {
            throw new IllegalArgumentException("A GRN must contain at least one receipt line");
        }

        Instant occurredAt = grn.receivedAt() == null ? Instant.now(clock) : grn.receivedAt();
        String correlationId = currentHeader("X-Correlation-Id");
        String traceId = traceIdFromTraceparent(currentHeader("traceparent"));

        for (GRNLine line : grn.items()) {
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", eventId);
            envelope.put("eventType", EVENT_TYPE);
            envelope.put("eventVersion", EVENT_VERSION);
            envelope.put("occurredAt", occurredAt);
            envelope.put("producer", PRODUCER);
            envelope.put("correlationId", correlationId);
            envelope.put("traceId", traceId);
            envelope.put("payload", payload(grn, line));

            try {
                String serialized = objectMapper.writeValueAsString(envelope);
                repository.save(new OutboxEvent(
                        eventId,
                        "GRN",
                        grn.grnId(),
                        EVENT_TYPE,
                        EVENT_VERSION,
                        topic,
                        grn.grnId(),
                        serialized,
                        Instant.now(clock)));
                if (metrics != null) metrics.outboxCreated();
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Unable to serialize GOODS_RECEIVED event", ex);
            }
        }
    }

    /**
     * Adds a durable audit record to the same transactional outbox used by
     * business events. This keeps the GRN mutation and its audit trail in the
     * same MongoDB transaction.
     */
    public void enqueueGRNCreateAuditEvent(GRNResponse grn) {
        Instant occurredAt = grn.receivedAt() == null ? Instant.now(clock) : grn.receivedAt();
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", AUDIT_EVENT_TYPE);
        envelope.put("eventVersion", EVENT_VERSION);
        envelope.put("occurredAt", occurredAt);
        envelope.put("producer", PRODUCER);
        envelope.put("correlationId", currentHeader("X-Correlation-Id"));
        envelope.put("traceId", traceIdFromTraceparent(currentHeader("traceparent")));
        envelope.put("payload", auditPayload(grn, occurredAt));

        try {
            String serialized = objectMapper.writeValueAsString(envelope);
            repository.save(new OutboxEvent(
                    eventId,
                    "GRN",
                    grn.grnId(),
                    AUDIT_EVENT_TYPE,
                    EVENT_VERSION,
                    topic,
                    grn.grnId(),
                    serialized,
                    Instant.now(clock)));
            if (metrics != null) metrics.outboxCreated();
            if (metrics != null) metrics.auditEventCreated();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize GRN audit event", ex);
        }
    }

    private Map<String, Object> auditPayload(GRNResponse grn, Instant occurredAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actorId = authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : "system";

        payload.put("actorId", actorId);
        payload.put("actorRoles", actorRoles(authentication));
        payload.put("service", PRODUCER);
        payload.put("entityType", "GRN");
        payload.put("entityId", grn.grnId());
        payload.put("action", "GRN_CREATED");
        payload.put("timestamp", occurredAt);
        payload.put("beforeStatus", null);
        payload.put("afterStatus", grn.grnStatus() == null ? null : grn.grnStatus().name());
        payload.put("poId", grn.poId());
        payload.put("deliveryNoteNumber", grn.deliveryNoteNumber());
        payload.put("receivedBy", grn.receivedBy());
        return payload;
    }

    private List<String> actorRoles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of("SYSTEM");
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
    }

    private Map<String, Object> payload(GRNResponse grn, GRNLine line) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grnId", grn.grnId());
        payload.put("poId", grn.poId());
        payload.put("itemId", line.itemId());
        payload.put("orderedQuantity", line.orderedQuantity());
        payload.put("acceptedQuantity", line.acceptedQuantity());
        payload.put("rejectedQuantity", line.rejectedQuantity());
        payload.put("unitOfMeasure", line.unitOfMeasure());
        payload.put("rejectionReason", line.rejectionReason());
        payload.put("deliveryNoteNumber", grn.deliveryNoteNumber());
        payload.put("deliveryDate", grn.deliveryDate());
        payload.put("receivedAt", grn.receivedAt());
        payload.put("receivedBy", grn.receivedBy());
        payload.put("vendorName", grn.vendorName());
        return payload;
    }

    private String currentHeader(String name) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        String value = attributes.getRequest().getHeader(name);
        return StringUtils.hasText(value) ? value : null;
    }

    private String traceIdFromTraceparent(String traceparent) {
        if (!StringUtils.hasText(traceparent)) {
            return null;
        }
        String[] parts = traceparent.trim().split("-");
        return parts.length == 4 && parts[1].matches("[0-9a-fA-F]{32}") ? parts[1] : null;
    }
}
