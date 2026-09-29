package com.recnaile.outward.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.outward.domain.model.OutwardIssueTransaction;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionalOutboxService {

    public static final String OUTWARD_ISSUED = "OUTWARD_ISSUED";
    public static final int EVENT_VERSION = 1;

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public TransactionalOutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public OutboxEvent enqueueOutwardIssued(OutwardIssueTransaction transaction) {
        String eventId = UUID.randomUUID().toString();
        Instant occurredAt = transaction.getIssuedAt();
        OutwardIssuedEventPayload payload = new OutwardIssuedEventPayload(
                eventId,
                OUTWARD_ISSUED,
                EVENT_VERSION,
                occurredAt,
                transaction.getOutwardNumber(),
                transaction.getRequestedBy(),
                transaction.getIssuerId(),
                transaction.getApproverId(),
                transaction.getDepartmentName(),
                transaction.getProjectId(),
                transaction.getRecipientEmployeeId(),
                transaction.getRecipientEmployeeName(),
                transaction.getPurpose(),
                transaction.isAssetTracked(),
                transaction.getItems());

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setEventType(OUTWARD_ISSUED);
        event.setAggregateType("OUTWARD_REQUEST");
        event.setAggregateId(transaction.getOutwardNumber());
        event.setPayload(toJson(payload));
        event.setStatus(OutboxEventStatus.PENDING);
        event.setOccurredAt(occurredAt);
        event.setCreatedAt(Instant.now());
        event.setAttemptCount(0);
        return repository.save(event);
    }

    public OutboxEvent enqueueAudit(AuditEventPayload payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventId(payload.eventId());
        event.setEventType(AuditEventService.AUDIT_EVENT);
        event.setAggregateType(payload.entityType());
        event.setAggregateId(payload.entityId());
        event.setPayload(toJson(payload));
        event.setStatus(OutboxEventStatus.PENDING);
        event.setOccurredAt(payload.occurredAt());
        event.setCreatedAt(Instant.now());
        event.setAttemptCount(0);
        return repository.save(event);
    }

    private String toJson(AuditEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize audit outbox payload", ex);
        }
    }

    private String toJson(OutwardIssuedEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize OUTWARD_ISSUED outbox payload", ex);
        }
    }
}
