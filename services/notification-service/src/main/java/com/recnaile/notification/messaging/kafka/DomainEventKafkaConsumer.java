package com.recnaile.notification.messaging.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.notification.application.DomainEventEnvelopeValidator;
import com.recnaile.notification.application.EventIdempotencyService;
import com.recnaile.notification.application.IncomingDomainEventInbox;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Ingests approved procurement domain events. The durable inbox is written before ack. */
@Component
public class DomainEventKafkaConsumer {
    private final ObjectMapper objectMapper;
    private final DomainEventEnvelopeValidator validator;
    private final IncomingDomainEventInbox inbox;
    private final EventIdempotencyService idempotencyService;

    public DomainEventKafkaConsumer(ObjectMapper objectMapper, DomainEventEnvelopeValidator validator,
            IncomingDomainEventInbox inbox, EventIdempotencyService idempotencyService) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.inbox = Objects.requireNonNull(inbox, "inbox");
        this.idempotencyService = Objects.requireNonNull(idempotencyService, "idempotencyService");
    }

    @KafkaListener(topics = {
            "${app.kafka.topics.indent}", "${app.kafka.topics.purchase-order}",
            "${app.kafka.topics.inward}", "${app.kafka.topics.inventory}",
            "${app.kafka.topics.outward}", "${app.kafka.topics.return}",
            "${app.kafka.topics.asset}"
    }, containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws Exception {
        if (record.value() == null || record.value().isBlank())
            throw new IllegalArgumentException("EMPTY_EVENT_MESSAGE");
        JsonNode root = objectMapper.readTree(record.value());
        DomainEventEnvelopeValidator.ValidatedEnvelope envelope = validator.validate(root);
        Map<String, Object> raw = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() { });
        Instant now = Instant.now();
        // Save durable inbox first. On a retry, duplicate event IDs resolve to the existing record.
        inbox.saveIfAbsent(envelope.eventId(), envelope.eventType(), record.topic(), record.partition(),
                record.offset(), now, raw);
        // Keep the separate processing ledger aligned for subsequent notification orchestration.
        idempotencyService.registerIfNew(envelope.eventId(), envelope.eventType(), now);
        acknowledgment.acknowledge();
    }
}
