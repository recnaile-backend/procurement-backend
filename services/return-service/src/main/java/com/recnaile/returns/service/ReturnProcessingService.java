package com.recnaile.returns.service;

import com.recnaile.returns.event.ReturnEventPublisher;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnProcessingService {

    private final ReturnRequestRepository repository;
    private final ReturnStateMachine stateMachine;
    private final Clock clock;
    private final ReturnEventPublisher eventPublisher;

    @Autowired
    public ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            ReturnEventPublisher eventPublisher) {
        this(repository, stateMachine, Clock.systemUTC(), eventPublisher);
    }

    ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock) {
        this(repository, stateMachine, clock, returnRequest -> { });
    }

    ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock,
            ReturnEventPublisher eventPublisher) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    public ReturnRequest process(String returnId) {
        String normalizedReturnId = normalizeReturnId(returnId);
        ReturnRequest returnRequest = repository.findById(normalizedReturnId)
                .orElseThrow(() -> new ReturnNotFoundException(normalizedReturnId));

        stateMachine.process(returnRequest);
        returnRequest.setUpdatedAt(Instant.now(clock));
        ReturnRequest saved = repository.save(returnRequest);
        eventPublisher.publish(saved);
        return saved;
    }

    private String normalizeReturnId(String returnId) {
        if (returnId == null || returnId.trim().isEmpty()) {
            throw new IllegalArgumentException("returnId must not be blank");
        }
        return returnId.trim();
    }
}
