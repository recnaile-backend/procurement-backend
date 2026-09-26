package com.recnaile.inward.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.recnaile.inward.model.OutboxEvent;
import com.recnaile.inward.repository.OutboxEventRepository;
import com.recnaile.inward.observability.InwardMetrics;

@Service
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;
    private final int batchSize;
    private final long retryDelaySeconds;
    private final InwardMetrics metrics;

    @Autowired
    public OutboxPublisher(
            OutboxEventRepository repository,
            MongoTemplate mongoTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock clock,
            @Value("${app.outbox.publisher.batch-size:100}") int batchSize,
            @Value("${app.outbox.publisher.retry-delay-seconds:30}") long retryDelaySeconds) {
        this(repository, mongoTemplate, kafkaTemplate, clock, batchSize, retryDelaySeconds, null);
    }

    public OutboxPublisher(
            OutboxEventRepository repository,
            MongoTemplate mongoTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock clock,
            int batchSize,
            long retryDelaySeconds,
            InwardMetrics metrics) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.batchSize = batchSize;
        this.retryDelaySeconds = retryDelaySeconds;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:5000}")
    public void publishPending() {
        Instant now = Instant.now(clock);
        List<OutboxEvent> candidates = repository.findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                List.of(OutboxEvent.Status.PENDING, OutboxEvent.Status.FAILED), now);

        candidates.stream().limit(batchSize).forEach(event -> {
            OutboxEvent claimed = claim(event.getId(), now);
            if (claimed == null) {
                return;
            }
            publish(claimed);
        });
    }

    private OutboxEvent claim(String id, Instant now) {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("status").in(OutboxEvent.Status.PENDING, OutboxEvent.Status.FAILED),
                Criteria.where("nextAttemptAt").lte(now)));
        Update update = new Update().set("status", OutboxEvent.Status.PUBLISHING)
                .inc("attemptCount", 1)
                .unset("lastError")
                .currentDate("updatedAt");
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), OutboxEvent.class);
    }

    private void publish(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()).get();
            event.markPublished(Instant.now(clock));
            repository.save(event);
            if (metrics != null) metrics.outboxPublished();
        } catch (Exception ex) {
            String detail = ex.getCause() != null && ex.getCause().getMessage() != null
                    ? ex.getCause().getMessage() : ex.getMessage();
            Instant retryAt = Instant.now(clock).plus(Duration.ofSeconds(retryDelaySeconds));
            event.markFailed(detail == null ? "Kafka publish failed" : detail, retryAt);
            repository.save(event);
            if (metrics != null) metrics.outboxFailed();
        }
    }
}
