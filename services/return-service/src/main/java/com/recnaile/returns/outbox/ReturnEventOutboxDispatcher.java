package com.recnaile.returns.outbox;

import com.recnaile.returns.event.KafkaReturnEventPublisher;
import com.recnaile.returns.model.enums.OutboxStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        name = "app.outbox.dispatch-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ReturnEventOutboxDispatcher {

    private final ReturnEventOutboxRepository repository;
    private final KafkaReturnEventPublisher publisher;
    private final Clock clock;

    @Autowired
    public ReturnEventOutboxDispatcher(
            ReturnEventOutboxRepository repository,
            KafkaReturnEventPublisher publisher) {
        this(repository, publisher, Clock.systemUTC());
    }

    ReturnEventOutboxDispatcher(
            ReturnEventOutboxRepository repository,
            KafkaReturnEventPublisher publisher,
            Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:5000}")
    public void dispatchPending() {
        List<ReturnEventOutbox> pending =
                repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        pending.forEach(this::dispatchOne);
    }

    void dispatchOne(ReturnEventOutbox outbox) {
        try {
            publisher.publish(
                    outbox.getTopic(),
                    outbox.getAggregateId(),
                    outbox.getPayload())
                    .get(10, TimeUnit.SECONDS);
            outbox.markSent(Instant.now(clock));
        } catch (Exception exception) {
            outbox.markAttemptFailed(safeMessage(exception));
        }
        repository.save(outbox);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
