package com.recnaile.outward.service;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.dto.OutwardResponse;
import com.recnaile.outward.dto.OutwardItemResponse;
import com.recnaile.outward.dto.RecipientResponse;
import com.recnaile.outward.exception.OutwardException;
import com.recnaile.outward.repository.OutwardRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class OutwardApprovalService {

    private final OutwardRequestRepository repository;
    private final OutwardStateMachine stateMachine;
    private final ApprovalPolicy approvalPolicy;
    private final Clock clock;

    @Autowired
    public OutwardApprovalService(OutwardRequestRepository repository,
                                  OutwardStateMachine stateMachine,
                                  ApprovalPolicy approvalPolicy) {
        this(repository, stateMachine, approvalPolicy, Clock.systemUTC());
    }

    OutwardApprovalService(OutwardRequestRepository repository,
                           OutwardStateMachine stateMachine,
                           ApprovalPolicy approvalPolicy,
                           Clock clock) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.approvalPolicy = approvalPolicy;
        this.clock = clock;
    }

    public OutwardResponse submit(String outwardNumber, Authentication authentication) {
        String actor = authenticatedName(authentication);
        OutwardRequest request = find(outwardNumber);
        if (!actor.equals(request.getRequestedBy())) {
            throw new OutwardException("OUTWARD_REQUEST_OWNER_REQUIRED",
                    "Only the requester can submit this outward request");
        }
        request.setStatus(stateMachine.transition(request.getStatus(), OutwardStatus.PENDING_APPROVAL));
        return save(request, Instant.now(clock));
    }

    public OutwardResponse approve(String outwardNumber, Authentication authentication) {
        String actor = authenticatedName(authentication);
        approvalPolicy.validate(authentication);
        OutwardRequest request = find(outwardNumber);
        request.setStatus(stateMachine.transition(request.getStatus(), OutwardStatus.APPROVED));
        request.setApprovedBy(actor);
        request.setApprovedAt(Instant.now(clock));
        request.setRejectedBy(null);
        request.setRejectedAt(null);
        request.setRejectionReason(null);
        return save(request, Instant.now(clock));
    }

    public OutwardResponse reject(String outwardNumber, String reason, Authentication authentication) {
        String actor = authenticatedName(authentication);
        approvalPolicy.validate(authentication);
        OutwardRequest request = find(outwardNumber);
        request.setStatus(stateMachine.transition(request.getStatus(), OutwardStatus.REJECTED));
        request.setRejectedBy(actor);
        request.setRejectedAt(Instant.now(clock));
        request.setRejectionReason(trimReason(reason));
        request.setApprovedBy(null);
        request.setApprovedAt(null);
        return save(request, Instant.now(clock));
    }

    private OutwardResponse save(OutwardRequest request, Instant now) {
        request.setUpdatedAt(now);
        try {
            return toResponse(repository.save(request));
        } catch (OptimisticLockingFailureException ex) {
            throw new OutwardException("OUTWARD_CONCURRENT_MODIFICATION",
                    "Outward request was modified by another operation; retry the request", ex);
        }
    }

    private OutwardRequest find(String outwardNumber) {
        if (outwardNumber == null || outwardNumber.isBlank()) {
            throw new OutwardException("OUTWARD_NUMBER_REQUIRED", "Outward number is required");
        }
        return repository.findByOutwardNumber(outwardNumber)
                .orElseThrow(() -> new OutwardException("OUTWARD_NOT_FOUND", "Outward request not found"));
    }

    private String authenticatedName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new OutwardException("AUTHENTICATION_REQUIRED", "Authenticated user is required");
        }
        return authentication.getName();
    }

    private String trimReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private OutwardResponse toResponse(OutwardRequest entity) {
        List<OutwardItemResponse> items = entity.getItems().stream()
                .map(item -> new OutwardItemResponse(item.getItemId(), item.getQuantity()))
                .toList();
        var recipient = entity.getRecipient();
        return new OutwardResponse(
                entity.getId(), entity.getOutwardNumber(), entity.getRequestedAt(), items,
                entity.getProjectId(), entity.getRequestedBy(), entity.getDepartmentName(), entity.getPurpose(),
                new RecipientResponse(recipient.getEmployeeId(), recipient.getEmployeeName()),
                entity.getStatus(), entity.getApprovedBy(), entity.getApprovedAt(),
                entity.getRejectedBy(), entity.getRejectedAt(), entity.getRejectionReason(),
                entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
