package com.recnaile.outward.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;
    private final String outwardIssuedTopic;
    private final String auditTopic;
    private final String notificationTopic;
    private final long sendTimeoutMs;
    private final int maxAttempts;

    @Autowired
    public OutboxEventPublisher(OutboxEventRepository repository,
                                KafkaTemplate<String, String> kafkaTemplate,
                                @Value("${app.kafka.topics.outward-issued:outward.events}") String outwardIssuedTopic,
                                @Value("${app.kafka.topics.audit:audit.events}") String auditTopic,
                                @Value("${app.kafka.topics.notification:notification.events}") String notificationTopic,
                                @Value("${app.kafka.publisher.send-timeout-ms:5000}") long sendTimeoutMs,
                                @Value("${app.kafka.publisher.max-attempts:10}") int maxAttempts) {
        this(repository, kafkaTemplate, outwardIssuedTopic, auditTopic, notificationTopic, sendTimeoutMs, maxAttempts, Clock.systemUTC());
    }

    OutboxEventPublisher(OutboxEventRepository repository,
                         KafkaTemplate<String, String> kafkaTemplate,
                         String outwardIssuedTopic,
                         String auditTopic,
                         String notificationTopic,
                         long sendTimeoutMs,
                         int maxAttempts,
                         Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.outwardIssuedTopic = outwardIssuedTopic;
        this.auditTopic = auditTopic;
        this.notificationTopic = notificationTopic;
        this.sendTimeoutMs = sendTimeoutMs;
        this.maxAttempts = maxAttempts;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.kafka.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);
        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    void publish(OutboxEvent event) {
        Instant now = Instant.now(clock);
        int attempts = event.getAttemptCount() == null ? 0 : event.getAttemptCount();
        attempts++;
        event.setAttemptCount(attempts);
        event.setLastAttemptAt(now);
        repository.save(event);

        try {
            String topic = topicFor(event.getEventType());
            SendResult<String, String> result = kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            RecordMetadata metadata = result.getRecordMetadata();
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now(clock));
            event.setLastError(null);
            repository.save(event);
            log.info("Published outbox event eventId={} eventType={} topic={} partition={} offset={}",
                    event.getEventId(), event.getEventType(), metadata.topic(), metadata.partition(), metadata.offset());
        } catch (Exception ex) {
            event.setLastError(safeError(ex));
            if (attempts >= maxAttempts) {
                event.setStatus(OutboxEventStatus.FAILED);
            }
            repository.save(event);
            log.warn("Failed to publish outbox event eventId={} attempt={} status={}",
                    event.getEventId(), attempts, event.getStatus(), ex);
        }
    }

    private String topicFor(String eventType) {
        if (AuditEventService.AUDIT_EVENT.equals(eventType)) {
            return auditTopic;
        }
        if (TransactionalOutboxService.OUTWARD_APPROVED.equals(eventType)
                || TransactionalOutboxService.OUTWARD_REJECTED.equals(eventType)) {
            return notificationTopic;
        }
        return outwardIssuedTopic;
    }

    private String safeError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
