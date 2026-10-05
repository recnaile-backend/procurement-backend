package com.recnaile.returns.integration.outbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock; import java.time.Instant; import java.util.List; import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name="app.integration-outbox.dispatch-enabled",havingValue="true",matchIfMissing=true)
public class ReturnIntegrationOutboxDispatcher {
    private final ReturnIntegrationOutboxRepository repository; private final KafkaReturnIntegrationPublisher publisher; private final Clock clock;
    @Autowired public ReturnIntegrationOutboxDispatcher(ReturnIntegrationOutboxRepository repository,KafkaReturnIntegrationPublisher publisher){this(repository,publisher,Clock.systemUTC());}
    ReturnIntegrationOutboxDispatcher(ReturnIntegrationOutboxRepository repository,KafkaReturnIntegrationPublisher publisher,Clock clock){this.repository=repository;this.publisher=publisher;this.clock=clock;}
    @Scheduled(fixedDelayString="${app.integration-outbox.dispatch-interval-ms:5000}") public void dispatchPending(){List<ReturnIntegrationOutbox> pending=repository.findTop50ByStatusOrderByCreatedAtAsc(IntegrationOutboxStatus.PENDING); pending.forEach(this::dispatchOne);}
    void dispatchOne(ReturnIntegrationOutbox outbox){try{publisher.publish(outbox.getTopic(),outbox.getAggregateId(),outbox.getPayload()).get(10,TimeUnit.SECONDS);outbox.markSent(Instant.now(clock));}catch(Exception ex){String m=ex.getMessage();outbox.markAttemptFailed(m==null||m.isBlank()?ex.getClass().getSimpleName():m);}repository.save(outbox);}
}
