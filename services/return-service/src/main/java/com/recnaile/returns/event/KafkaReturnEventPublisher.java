package com.recnaile.returns.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Low-level JSON publisher retained for operational/recovery use.
 * Normal business processing persists an outbox first and the dispatcher
 * publishes the serialized payload.
 */
@Component
public class KafkaReturnEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaReturnEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<?> publishReturnRecorded(String topic, ReturnRequest returnRequest) {
        return send(topic, returnRequest.getReturnId(), ReturnRecordedEvent.create(returnRequest));
    }

    public CompletableFuture<?> publishReturnAccepted(String topic, ReturnRequest returnRequest) {
        return send(topic, returnRequest.getReturnId(), ReturnAcceptedEvent.create(returnRequest));
    }

    public CompletableFuture<?> publish(String topic, String key, String payload) {
        return kafkaTemplate.send(topic, key, payload);
    }

    private CompletableFuture<?> send(String topic, String key, Object event) {
        try {
            return kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize return integration event", exception);
        }
    }
}
