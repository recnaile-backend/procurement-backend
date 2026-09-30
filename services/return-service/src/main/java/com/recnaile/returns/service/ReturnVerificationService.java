package com.recnaile.returns.service;

import com.recnaile.returns.dto.VerifyReturnRequest;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recnaile.returns.idempotency.IdempotencyService;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnVerificationService {

    private final ReturnRequestRepository repository;
    private final ReturnStateMachine stateMachine;
    private final Clock clock;
    private final IdempotencyService idempotencyService;

    @Autowired
    public ReturnVerificationService(ReturnRequestRepository repository, ReturnStateMachine stateMachine, IdempotencyService idempotencyService) {
        this(repository, stateMachine, Clock.systemUTC(), idempotencyService);
    }

    ReturnVerificationService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock) {
        this(repository, stateMachine, clock, null);
    }

    ReturnVerificationService(ReturnRequestRepository repository, ReturnStateMachine stateMachine, Clock clock, IdempotencyService idempotencyService) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
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
            ReturnRequest returnRequest = repository.findById(normalizedReturnId)
                    .orElseThrow(() -> new ReturnNotFoundException(normalizedReturnId));
            stateMachine.verify(returnRequest);
            Instant verifiedAt = Instant.now(clock);
            returnRequest.setVerifiedBy(verifier);
            returnRequest.setVerifiedAt(verifiedAt);
            returnRequest.setUpdatedAt(verifiedAt);
            ReturnRequest saved = repository.save(returnRequest);
            idempotencyService.record(key, "VERIFY_RETURN", verifier, fingerprint, saved.getReturnId());
            return saved;
        }

        ReturnRequest returnRequest = repository.findById(normalizedReturnId)
                .orElseThrow(() -> new ReturnNotFoundException(normalizedReturnId));

        stateMachine.verify(returnRequest);
        Instant verifiedAt = Instant.now(clock);
        returnRequest.setVerifiedBy(verifier);
        returnRequest.setVerifiedAt(verifiedAt);
        returnRequest.setUpdatedAt(verifiedAt);

        return repository.save(returnRequest);
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
