package com.recnaile.outward.service;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardIssueTransaction;
import com.recnaile.outward.domain.model.OutwardItem;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.dto.OutwardIssueTransactionResponse;
import com.recnaile.outward.dto.OutwardItemResponse;
import com.recnaile.outward.exception.OutwardException;
import com.recnaile.outward.repository.OutwardIssueTransactionRepository;
import com.recnaile.outward.repository.OutwardRequestRepository;
import com.recnaile.outward.outbox.TransactionalOutboxService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutwardIssueService {

    private final OutwardRequestRepository requestRepository;
    private final OutwardIssueTransactionRepository issueRepository;
    private final OutwardStateMachine stateMachine;
    private final TransactionalOutboxService outboxService;
    private final com.recnaile.outward.outbox.AuditEventService auditEventService;
    private final Clock clock;

    @Autowired
    public OutwardIssueService(OutwardRequestRepository requestRepository,
                               OutwardIssueTransactionRepository issueRepository,
                               OutwardStateMachine stateMachine,
                               TransactionalOutboxService outboxService,
                               com.recnaile.outward.outbox.AuditEventService auditEventService) {
        this(requestRepository, issueRepository, stateMachine, outboxService, auditEventService, Clock.systemUTC());
    }

    OutwardIssueService(OutwardRequestRepository requestRepository,
                        OutwardIssueTransactionRepository issueRepository,
                        OutwardStateMachine stateMachine,
                        TransactionalOutboxService outboxService,
                        com.recnaile.outward.outbox.AuditEventService auditEventService,
                        Clock clock) {
        this.requestRepository = requestRepository;
        this.issueRepository = issueRepository;
        this.stateMachine = stateMachine;
        this.outboxService = outboxService;
        this.auditEventService = auditEventService;
        this.clock = clock;
    }

    public OutwardIssueTransactionResponse issue(String outwardNumber, Authentication authentication) {
        return issue(outwardNumber, null, authentication);
    }

    @Transactional
    public OutwardIssueTransactionResponse issue(String outwardNumber, String idempotencyKey, Authentication authentication) {
        String issuerId = authenticatedName(authentication);
        if (outwardNumber == null || outwardNumber.isBlank()) {
            throw new OutwardException("OUTWARD_NUMBER_REQUIRED", "Outward number is required");
        }

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        if (normalizedKey != null) {
            OutwardIssueTransaction replay = issueRepository.findByIdempotencyKey(normalizedKey).orElse(null);
            if (replay != null) {
                if (!outwardNumber.equals(replay.getOutwardNumber())) {
                    throw new OutwardException("OUTWARD_IDEMPOTENCY_KEY_REUSED",
                            "Idempotency key was already used for another outward request");
                }
                return toResponse(replay);
            }
        }

        OutwardRequest request = requestRepository.findByOutwardNumber(outwardNumber)
                .orElseThrow(() -> new OutwardException("OUTWARD_NOT_FOUND", "Outward request not found"));

        if (request.getStatus() != OutwardStatus.APPROVED) {
            throw new OutwardException("OUTWARD_NOT_APPROVED",
                    "Only an approved outward request can be issued");
        }

        if (issueRepository.findByOutwardNumber(outwardNumber).isPresent()) {
            throw new OutwardException("OUTWARD_ALREADY_ISSUED",
                    "An issue transaction already exists for this outward request");
        }

        Instant now = Instant.now(clock);
        OutwardIssueTransaction transaction = new OutwardIssueTransaction();
        transaction.setOutwardNumber(request.getOutwardNumber());
        transaction.setIdempotencyKey(normalizedKey);
        transaction.setIssuedAt(now);
        transaction.setItems(copyItems(request.getItems()));
        transaction.setProjectId(request.getProjectId());
        transaction.setRequestedBy(request.getRequestedBy());
        transaction.setDepartmentName(request.getDepartmentName());
        transaction.setRecipientEmployeeId(request.getRecipient().getEmployeeId());
        transaction.setRecipientEmployeeName(request.getRecipient().getEmployeeName());
        transaction.setIssuerId(issuerId);
        transaction.setApproverId(request.getApprovedBy());
        transaction.setPurpose(request.getPurpose());
        transaction.setAssetTracked(request.isAssetTracked());

        try {
            OutwardIssueTransaction saved = issueRepository.save(transaction);
            OutwardStatus before = request.getStatus();
            request.setStatus(stateMachine.transition(before, OutwardStatus.ISSUED));
            request.setUpdatedAt(now);
            requestRepository.save(request);
            outboxService.enqueueOutwardIssued(saved);
            auditEventService.record("OUTWARD_REQUEST", request.getOutwardNumber(), "ISSUE",
                    authentication, before.name(), request.getStatus().name(), null);
            return toResponse(saved);
        } catch (DuplicateKeyException ex) {
            throw new OutwardException("OUTWARD_ALREADY_ISSUED",
                    "An issue transaction already exists for this outward request", ex);
        } catch (OptimisticLockingFailureException ex) {
            throw new OutwardException("OUTWARD_CONCURRENT_MODIFICATION",
                    "Outward request was modified by another operation; retry the request", ex);
        }
    }

    private List<OutwardItem> copyItems(List<OutwardItem> source) {
        if (source == null || source.isEmpty()) {
            throw new OutwardException("OUTWARD_ITEMS_REQUIRED", "At least one item is required to issue stock");
        }
        List<OutwardItem> copy = new ArrayList<>(source.size());
        for (OutwardItem item : source) {
            if (item == null || item.getItemId() == null || item.getItemId().isBlank()
                    || item.getQuantity() == null || item.getQuantity().signum() <= 0) {
                throw new OutwardException("OUTWARD_INVALID_ITEM",
                        "Issue transaction contains an invalid item or quantity");
            }
            copy.add(new OutwardItem(item.getItemId(), item.getQuantity()));
        }
        return copy;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 128) {
            throw new OutwardException("OUTWARD_IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key must not exceed 128 characters");
        }
        return normalized;
    }

    private String authenticatedName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new OutwardException("AUTHENTICATION_REQUIRED", "Authenticated user is required");
        }
        return authentication.getName();
    }

    private OutwardIssueTransactionResponse toResponse(OutwardIssueTransaction entity) {
        List<OutwardItemResponse> items = entity.getItems().stream()
                .map(item -> new OutwardItemResponse(item.getItemId(), item.getQuantity()))
                .toList();
        return new OutwardIssueTransactionResponse(
                entity.getId(), entity.getOutwardNumber(), entity.getIdempotencyKey(), entity.getIssuedAt(), items,
                entity.getProjectId(), entity.getRequestedBy(), entity.getDepartmentName(),
                entity.getRecipientEmployeeId(), entity.getRecipientEmployeeName(),
                entity.getIssuerId(), entity.getApproverId(), entity.getPurpose(),
                entity.isAssetTracked(), entity.getVersion(), entity.getCreatedAt());
    }
}
