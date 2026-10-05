package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
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
public class ReturnRequestService {

    private final ReturnRequestRepository repository;
    private final IssuedReturnValidationService issuedReturnValidationService;
    private final Clock clock;
    private final IdempotencyService idempotencyService;
    private final ReturnIntegrationOutboxService integrationOutboxService;

    @Autowired
    public ReturnRequestService(ReturnRequestRepository repository, IssuedReturnValidationService issuedReturnValidationService, IdempotencyService idempotencyService, ReturnIntegrationOutboxService integrationOutboxService) {
        this(repository, issuedReturnValidationService, Clock.systemUTC(), idempotencyService, integrationOutboxService);
    }

    ReturnRequestService(ReturnRequestRepository repository, IssuedReturnValidationService issuedReturnValidationService, Clock clock) {
        this(repository, issuedReturnValidationService, clock, null, null);
    }

    ReturnRequestService(ReturnRequestRepository repository, IssuedReturnValidationService issuedReturnValidationService, Clock clock, IdempotencyService idempotencyService) {
        this(repository, issuedReturnValidationService, clock, idempotencyService, null);
    }

    ReturnRequestService(ReturnRequestRepository repository, IssuedReturnValidationService issuedReturnValidationService, Clock clock, IdempotencyService idempotencyService, ReturnIntegrationOutboxService integrationOutboxService) {
        this.repository = repository;
        this.issuedReturnValidationService = issuedReturnValidationService;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
        this.integrationOutboxService = integrationOutboxService;
    }

    public ReturnRequest create(CreateReturnRequest request) {
        return create(request, request == null ? null : request.returnedBy(), null);
    }

    @Transactional
    public ReturnRequest create(CreateReturnRequest request, String actor, String idempotencyKey) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String normalizedActor = normalizeActor(actor, request.returnedBy());
        if (!normalizedActor.equals(request.returnedBy().trim())) {
            throw new IllegalArgumentException("returnedBy must match the authenticated user");
        }
        if (idempotencyService == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            issuedReturnValidationService.validate(request);
            ReturnRequest created = persist(request, normalizedActor);
            enqueueIntegrations(created, normalizedActor, "CREATE", null, created.getStatus().name(), null);
            return created;
        }
        String key = idempotencyService.normalizeKey(idempotencyKey);
        String fingerprint = idempotencyService.hash(request.toString());
        var existing = idempotencyService.findExisting(key, "CREATE_RETURN", normalizedActor, fingerprint);
        if (existing.isPresent()) {
            return existing.get();
        }
        issuedReturnValidationService.validate(request);
        ReturnRequest created = persist(request, normalizedActor);
        enqueueIntegrations(created, normalizedActor, "CREATE", null, created.getStatus().name(), null);
        idempotencyService.record(key, "CREATE_RETURN", normalizedActor, fingerprint, created.getReturnId());
        return created;
    }

    private ReturnRequest persist(CreateReturnRequest request, String actor) {
        ReturnRequest returnRequest = new ReturnRequest(
                request.outwardId().trim(), request.outwardLineId().trim(), request.itemId().trim(),
                request.quantity(), actor, request.condition(), normalizeRemarks(request.remarks()),
                normalizeOptional(request.assetId()), Instant.now(clock));
        return repository.save(returnRequest);
    }

    private void enqueueIntegrations(ReturnRequest r, String actor, String action, String previous, String next, String correlationId) {
        if (integrationOutboxService == null) return;
        integrationOutboxService.audit(r.getReturnId(), actor, null, action, previous, next, correlationId);
        integrationOutboxService.notify(r.getReturnId(), actor, action, "Return " + action.toLowerCase() + " successfully", correlationId);
    }

    private String normalizeActor(String actor, String fallbackActor) {
        String normalizedActor = normalizeOptional(actor);
        if (normalizedActor != null) {
            return normalizedActor;
        }
        String normalizedFallback = normalizeOptional(fallbackActor);
        if (normalizedFallback == null) {
            throw new IllegalArgumentException("authenticated actor must not be blank");
        }
        return normalizedFallback;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRemarks(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
