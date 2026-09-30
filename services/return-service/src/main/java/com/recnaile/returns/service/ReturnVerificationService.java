package com.recnaile.returns.service;

import com.recnaile.returns.dto.VerifyReturnRequest;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnVerificationService {

    private final ReturnRequestRepository repository;
    private final ReturnStateMachine stateMachine;
    private final Clock clock;

    @Autowired
    public ReturnVerificationService(ReturnRequestRepository repository, ReturnStateMachine stateMachine) {
        this(repository, stateMachine, Clock.systemUTC());
    }

    ReturnVerificationService(
            ReturnRequestRepository repository,
            ReturnStateMachine stateMachine,
            Clock clock) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    public ReturnRequest verify(String returnId, VerifyReturnRequest request) {
        String normalizedReturnId = normalizeReturnId(returnId);
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        String verifier = normalizeVerifier(request.verifiedBy());
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
