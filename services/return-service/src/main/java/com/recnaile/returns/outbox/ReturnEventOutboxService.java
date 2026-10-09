package com.recnaile.returns.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.returns.event.ReturnRecordedEvent;
import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnEventOutboxService {

    private final ReturnEventOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final Clock clock;

    @Autowired
    public ReturnEventOutboxService(
            ReturnEventOutboxRepository repository,
            ObjectMapper objectMapper,
            Environment environment) {
        this(repository, objectMapper, environment, Clock.systemUTC());
    }

    ReturnEventOutboxService(
            ReturnEventOutboxRepository repository,
            ObjectMapper objectMapper,
            Environment environment,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.clock = clock;
    }

    public ReturnEventOutbox enqueue(ReturnRequest returnRequest) {
        return enqueueReturnRecorded(returnRequest);
    }

    public ReturnEventOutbox enqueueReturnRecorded(ReturnRequest returnRequest) {
        ReturnRecordedEvent event = ReturnRecordedEvent.create(requireRequest(returnRequest));
        return save(event.eventId(), event.eventType(), returnRequest, event,
                environment.getProperty("app.kafka.topics.return-recorded", "return.recorded"));
    }

    private ReturnEventOutbox save(
            String eventId,
            String eventType,
            ReturnRequest returnRequest,
            Object event,
            String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            Instant now = Instant.now(clock);
            ReturnEventOutbox outbox = new ReturnEventOutbox(
                    eventId,
                    returnRequest.getReturnId(),
                    eventType,
                    topic,
                    returnRequest.getReturnId(),
                    returnRequest.getOutwardId(),
                    returnRequest.getItemId(),
                    returnRequest.getQuantity(),
                    returnRequest.getCondition().name(),
                    payload,
                    returnRequest.getUpdatedAt(),
                    now);
            return repository.save(outbox);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize return integration event", exception);
        }
    }

    private ReturnRequest requireRequest(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("returnRequest must not be null");
        }
        if (returnRequest.getReturnId() == null || returnRequest.getReturnId().isBlank()) {
            throw new IllegalArgumentException("returnId must not be blank");
        }
        if (returnRequest.getOutwardId() == null || returnRequest.getOutwardId().isBlank()) {
            throw new IllegalArgumentException("outwardId must not be blank");
        }
        if (returnRequest.getItemId() == null || returnRequest.getItemId().isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (returnRequest.getQuantity() == null || returnRequest.getQuantity() < 1) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (returnRequest.getCondition() == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        if (returnRequest.getUpdatedAt() == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
        return returnRequest;
    }
}
