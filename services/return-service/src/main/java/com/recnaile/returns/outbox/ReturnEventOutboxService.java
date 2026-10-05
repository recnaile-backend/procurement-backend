package com.recnaile.returns.outbox;

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
    private final String topic;
    private final Clock clock;

    @Autowired
    public ReturnEventOutboxService(ReturnEventOutboxRepository repository, Environment environment) {
        this(repository, environment, Clock.systemUTC());
    }

    ReturnEventOutboxService(ReturnEventOutboxRepository repository, Environment environment, Clock clock) {
        this.repository = repository;
        this.topic = environment.getProperty("app.kafka.topics.return-recorded", "return.recorded");
        this.clock = clock;
    }

    public ReturnEventOutbox enqueue(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("returnRequest must not be null");
        }
        ReturnRecordedEvent event = ReturnRecordedEvent.create(returnRequest);
        ReturnEventOutbox outbox = new ReturnEventOutbox(
                event.getEventId(),
                returnRequest.getReturnId(),
                event.getEventType(),
                topic,
                event.getReturnId(),
                event.getItemId(),
                event.getQty(),
                event.getCondition(),
                event.getOccurredAt(),
                Instant.now(clock));
        return repository.save(outbox);
    }
}
