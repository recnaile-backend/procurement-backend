package com.recnaile.returns.service;

import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.idempotency.IdempotencyService;
import com.recnaile.returns.integration.outbox.ReturnIntegrationOutboxService;
import com.recnaile.returns.reservation.ReturnAllocationService;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.outbox.ReturnEventOutboxService;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnProcessingService {

    private final ReturnRequestRepository repository;
    private final ReturnStateMachine stateMachine;
    private final Clock clock;
    private final ReturnEventOutboxService outboxService;
    private final IssuedReturnValidationService issuedReturnValidationService;
    private final IdempotencyService idempotencyService;
    private final ReturnIntegrationOutboxService integrationOutboxService;
    private final ReturnAllocationService returnAllocationService;

    @Autowired
    public ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            ReturnEventOutboxService outboxService,
            IssuedReturnValidationService issuedReturnValidationService,
            IdempotencyService idempotencyService,
            ReturnIntegrationOutboxService integrationOutboxService,
            ReturnAllocationService returnAllocationService) {
        this(
                repository,
                stateMachine,
                Clock.systemUTC(),
                outboxService,
                issuedReturnValidationService,
                idempotencyService,
                integrationOutboxService,
                returnAllocationService);
    }

    ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock,
            ReturnEventOutboxService outboxService) {
        this(repository, stateMachine, clock, outboxService, null, null, null, null);
    }

    ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock,
            ReturnEventOutboxService outboxService,
            IdempotencyService idempotencyService) {
        this(repository, stateMachine, clock, outboxService, null, idempotencyService, null, null);
    }

    ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock,
            ReturnEventOutboxService outboxService,
            IdempotencyService idempotencyService,
            ReturnIntegrationOutboxService integrationOutboxService) {
        this(repository, stateMachine, clock, outboxService, null, idempotencyService, integrationOutboxService, null);
    }

    ReturnProcessingService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock,
            ReturnEventOutboxService outboxService,
            IssuedReturnValidationService issuedReturnValidationService,
            IdempotencyService idempotencyService,
            ReturnIntegrationOutboxService integrationOutboxService,
            ReturnAllocationService returnAllocationService) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
        this.outboxService = outboxService;
        this.issuedReturnValidationService = issuedReturnValidationService;
        this.idempotencyService = idempotencyService;
        this.integrationOutboxService = integrationOutboxService;
        this.returnAllocationService = returnAllocationService;
    }

    @Transactional
    public ReturnRequest process(String returnId) {
        return process(returnId, "system", null);
    }

    @Transactional
    public ReturnRequest process(
            String returnId,
            String actor,
            String idempotencyKey) {
        String normalizedReturnId = normalizeReturnId(returnId);
        String normalizedActor = actor == null || actor.isBlank() ? "system" : actor.trim();

        if (idempotencyService != null && idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = idempotencyService.normalizeKey(idempotencyKey);
            String fingerprint = idempotencyService.hash(
                    normalizedReturnId + "|PROCESS_RETURN|" + normalizedActor);

            var existing = idempotencyService.findExisting(
                    key, "PROCESS_RETURN", normalizedActor, fingerprint);
            if (existing.isPresent()) {
                return existing.get();
            }
            idempotencyService.claim(key, "PROCESS_RETURN", normalizedActor, fingerprint);

            ReturnRequest saved = processAndPublish(normalizedReturnId, normalizedActor);
            idempotencyService.complete(
                    key, "PROCESS_RETURN", normalizedActor, fingerprint, saved.getReturnId());
            return saved;
        }

        return processAndPublish(normalizedReturnId, normalizedActor);
    }

    private ReturnRequest processAndPublish(String returnId, String actor) {
        ReturnRequest returnRequest = repository.findByReturnId(returnId)
                .orElseThrow(() -> new ReturnNotFoundException(returnId));

        if (issuedReturnValidationService != null) {
            var validation = issuedReturnValidationService.validate(returnRequest);
            returnRequest.setAssetTracked(validation.assetTracked());
            if (returnAllocationService != null) {
                returnAllocationService.reserve(returnRequest, validation.issuedQuantity());
            }
        }

        String previous = returnRequest.getStatus().name();
        stateMachine.process(returnRequest);
        Instant processedAt = Instant.now(clock);
        returnRequest.setUpdatedAt(processedAt);

        ReturnRequest saved = repository.save(returnRequest);

        /*
         * Both downstream events are written in the same MongoDB transaction
         * as the accepted Return state. Inventory always receives
         * RETURN_RECORDED; Asset receives ReturnAccepted only for reusable
         * asset-tracked items.
         */
        outboxService.enqueue(saved);
        outboxService.enqueueReturnAccepted(saved);

        enqueueIntegrations(
                saved,
                actor,
                "PROCESS",
                previous,
                saved.getStatus().name(),
                null);

        return saved;
    }

    private void enqueueIntegrations(
            ReturnRequest returnRequest,
            String actor,
            String action,
            String previous,
            String next,
            String correlationId) {
        if (integrationOutboxService == null) {
            return;
        }
        integrationOutboxService.audit(
                returnRequest.getReturnId(),
                actor,
                null,
                action,
                previous,
                next,
                correlationId);
        integrationOutboxService.notify(
                returnRequest.getReturnId(),
                actor,
                action,
                "Return processed as " + returnRequest.getCondition(),
                correlationId);
    }

    private String normalizeReturnId(String returnId) {
        if (returnId == null || returnId.trim().isEmpty()) {
            throw new IllegalArgumentException("returnId must not be blank");
        }
        return returnId.trim();
    }
}
