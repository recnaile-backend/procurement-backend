package com.recnaile.returns.service;

import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.outbox.ReturnEventOutboxService;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recnaile.returns.idempotency.IdempotencyService;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnProcessingService {

    private final ReturnRequestRepository repository;
    private final ReturnStateMachine stateMachine;
    private final Clock clock;
    private final ReturnEventOutboxService outboxService;
    private final IdempotencyService idempotencyService;

    @Autowired
    public ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            ReturnEventOutboxService outboxService, IdempotencyService idempotencyService) {
        this(repository, stateMachine, Clock.systemUTC(), outboxService, idempotencyService);
    }

    ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock,
            ReturnEventOutboxService outboxService) {
        this(repository, stateMachine, clock, outboxService, null);
    }

    ReturnProcessingService(
            ReturnRequestRepository repository, ReturnStateMachine stateMachine, Clock clock,
            ReturnEventOutboxService outboxService, IdempotencyService idempotencyService) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
        this.outboxService = outboxService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public ReturnRequest process(String returnId) {
        return process(returnId, "system", null);
    }

    @Transactional
    public ReturnRequest process(String returnId, String actor, String idempotencyKey) {
        String normalizedReturnId = normalizeReturnId(returnId);
        String normalizedActor = actor == null || actor.isBlank() ? "system" : actor.trim();
        if (idempotencyService != null && idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = idempotencyService.normalizeKey(idempotencyKey);
            String fingerprint = idempotencyService.hash(normalizedReturnId + "|PROCESS_RETURN");
            var existing = idempotencyService.findExisting(key, "PROCESS_RETURN", normalizedActor, fingerprint);
            if (existing.isPresent()) {
                return existing.get();
            }
            ReturnRequest returnRequest = repository.findById(normalizedReturnId)
                    .orElseThrow(() -> new ReturnNotFoundException(normalizedReturnId));
            stateMachine.process(returnRequest);
            returnRequest.setUpdatedAt(Instant.now(clock));
            ReturnRequest saved = repository.save(returnRequest);
            outboxService.enqueue(saved);
            idempotencyService.record(key, "PROCESS_RETURN", normalizedActor, fingerprint, saved.getReturnId());
            return saved;
        }

        ReturnRequest returnRequest = repository.findById(normalizedReturnId)
                .orElseThrow(() -> new ReturnNotFoundException(normalizedReturnId));

        stateMachine.process(returnRequest);
        returnRequest.setUpdatedAt(Instant.now(clock));
        ReturnRequest saved = repository.save(returnRequest);
        outboxService.enqueue(saved);
        return saved;
    }

    private String normalizeReturnId(String returnId) {
        if (returnId == null || returnId.trim().isEmpty()) {
            throw new IllegalArgumentException("returnId must not be blank");
        }
        return returnId.trim();
    }
}
