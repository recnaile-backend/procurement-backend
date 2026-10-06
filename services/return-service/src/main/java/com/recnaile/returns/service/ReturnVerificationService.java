package com.recnaile.returns.service;

import com.recnaile.returns.dto.VerifyReturnRequest;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recnaile.returns.idempotency.IdempotencyService;
import com.recnaile.returns.integration.outbox.ReturnIntegrationOutboxService;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnVerificationService {

    private final ReturnRequestRepository repository;
    private final ReturnStateMachine stateMachine;
    private final Clock clock;
    private final IdempotencyService idempotencyService;
    private final ReturnIntegrationOutboxService integrationOutboxService;

    @Autowired
    public ReturnVerificationService(ReturnRequestRepository repository, ReturnStateMachine stateMachine, IdempotencyService idempotencyService, ReturnIntegrationOutboxService integrationOutboxService) {
        this(repository, stateMachine, Clock.systemUTC(), idempotencyService, integrationOutboxService);
    }

    ReturnVerificationService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock) {
        this(repository, stateMachine, clock, null, null);
    }

    ReturnVerificationService(ReturnRequestRepository repository, ReturnStateMachine stateMachine, Clock clock, IdempotencyService idempotencyService) {
        this(repository, stateMachine, clock, idempotencyService, null);
    }

    ReturnVerificationService(ReturnRequestRepository repository, ReturnStateMachine stateMachine, Clock clock, IdempotencyService idempotencyService, ReturnIntegrationOutboxService integrationOutboxService) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
        this.integrationOutboxService = integrationOutboxService;
    }

    public ReturnRequest verify(String returnId, VerifyReturnRequest request) {
        return verify(returnId, request, request == null ? null : request.verifiedBy(), null);
    }

    @Transactional
    public ReturnRequest verify(String returnId, VerifyReturnRequest request, String actor, String idempotencyKey) {
        String normalizedReturnId = normalizeReturnId(returnId);
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        String verifier = normalizeVerifier(actor == null ? request.verifiedBy() : actor);
        if (actor != null && request.verifiedBy() != null
                && !verifier.equals(request.verifiedBy().trim())) {
            throw new IllegalArgumentException("verifiedBy must match the authenticated user");
        }
        if (idempotencyService != null && idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = idempotencyService.normalizeKey(idempotencyKey);
            String fingerprint = idempotencyService.hash(normalizedReturnId + "|VERIFY_RETURN|" + verifier);
            var existing = idempotencyService.findExisting(key, "VERIFY_RETURN", verifier, fingerprint);
            if (existing.isPresent()) {
                return existing.get();
            }
            ReturnRequest returnRequest = repository.findByReturnId(normalizedReturnId)
                    .orElseThrow(() -> new ReturnNotFoundException(normalizedReturnId));
            stateMachine.verify(returnRequest);
            Instant verifiedAt = Instant.now(clock);
            returnRequest.setVerifiedBy(verifier);
            returnRequest.setVerifiedAt(verifiedAt);
            returnRequest.setUpdatedAt(verifiedAt);
            ReturnRequest saved = repository.save(returnRequest);
            enqueueIntegrations(saved, verifier, "VERIFY", "PENDING_VERIFICATION", saved.getStatus().name(), null);
            idempotencyService.record(key, "VERIFY_RETURN", verifier, fingerprint, saved.getReturnId());
            return saved;
        }

        ReturnRequest returnRequest = repository.findByReturnId(normalizedReturnId)
                .orElseThrow(() -> new ReturnNotFoundException(normalizedReturnId));

        stateMachine.verify(returnRequest);
        Instant verifiedAt = Instant.now(clock);
        returnRequest.setVerifiedBy(verifier);
        returnRequest.setVerifiedAt(verifiedAt);
        returnRequest.setUpdatedAt(verifiedAt);

        ReturnRequest saved = repository.save(returnRequest);
        enqueueIntegrations(saved, verifier, "VERIFY", "PENDING_VERIFICATION", saved.getStatus().name(), null);
        return saved;
    }

    private void enqueueIntegrations(ReturnRequest r, String actor, String action, String previous, String next, String correlationId) {
        if (integrationOutboxService == null) return;
        integrationOutboxService.audit(r.getReturnId(), actor, null, action, previous, next, correlationId);
        integrationOutboxService.notify(r.getReturnId(), actor, action, "Return verified", correlationId);
    }

    private String normalizeReturnId(String returnId) {
        if (returnId == null || returnId.trim().isEmpty()) {
            throw new IllegalArgumentException("returnId must not be blank");
        }
        return returnId.trim();
    }

    private String normalizeVerifier(String verifiedBy) {
        if (verifiedBy == null || verifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("verifiedBy must not be blank");
        }
        return verifiedBy.trim();
    }
}
