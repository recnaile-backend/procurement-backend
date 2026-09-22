package com.recnaile.indent.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.micrometer.tracing.Tracer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.domain.model.OutboxEvent;
import com.recnaile.indent.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private static final String PRODUCER_SERVICE = "indent-service";
    private static final String AGGREGATE_TYPE = "INDENT";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    private static final String CORRELATION_ATTRIBUTE =
            OutboxEventService.class.getName() + ".correlationId";

    public OutboxEvent create(
            String eventType,
            String eventVersion,
            Instant occurredAt,
            String aggregateId,
            String correlationId,
            String causationId,
            String actorId,
            String actorRole,
            String topic,
            Object payload) {

        String serializedPayload = serializePayload(payload);

        Instant now = Instant.now();
        String resolvedCorrelationId = resolveCorrelationId(correlationId);
        String traceId = resolveTraceId();

        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventVersion(eventVersion)
                .occurredAt(occurredAt)
                .producerService(PRODUCER_SERVICE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .correlationId(resolvedCorrelationId)
                .traceId(traceId)
                .causationId(causationId)
                .actorId(actorId)
                .actorRole(actorRole)
                .topic(topic)
                .payload(serializedPayload)
                .status(OutboxEvent.OutboxStatus.PENDING)
                .attemptCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return outboxEventRepository.save(event);
    }

    private String resolveCorrelationId(String correlationId) {
        if (isUuid(correlationId)) {
            return correlationId;
        }

        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            Object existing = servletAttributes.getRequest()
                    .getAttribute(CORRELATION_ATTRIBUTE);
            if (existing instanceof String existingId && isUuid(existingId)) {
                return existingId;
            }

            String header = servletAttributes.getRequest()
                    .getHeader("X-Correlation-Id");
            if (isUuid(header)) {
                servletAttributes.getRequest()
                        .setAttribute(CORRELATION_ATTRIBUTE, header);
                return header;
            }

            String generated = UUID.randomUUID().toString();
            servletAttributes.getRequest()
                    .setAttribute(CORRELATION_ATTRIBUTE, generated);
            return generated;
        }

        return UUID.randomUUID().toString();
    }

    private String resolveTraceId() {
        var span = tracer.currentSpan();
        return span != null && span.context() != null
                ? span.context().traceId()
                : "00000000000000000000000000000000";
    }

    private boolean isUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize outbox event payload",
                    ex);
        }
    }
}