package com.recnaile.outward.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.recnaile.outward.client.inventory.InventoryClient;
import com.recnaile.outward.client.inventory.InventorySummaryResponse;
import com.recnaile.outward.client.masterdata.ItemResponse;
import com.recnaile.outward.client.masterdata.MasterDataClient;
import com.recnaile.outward.client.masterdata.ReferenceResponse;
import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.IssueTransaction;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.domain.state.OutwardStateMachine;
import com.recnaile.outward.dto.request.CancelOutwardRequest;
import com.recnaile.outward.dto.request.CreateOutwardRequest;
import com.recnaile.outward.dto.request.RejectOutwardRequest;
import com.recnaile.outward.dto.response.OutwardResponse;
import com.recnaile.outward.exception.ConcurrentOutwardModificationException;
import com.recnaile.outward.exception.InsufficientStockException;
import com.recnaile.outward.exception.OutwardNotFoundException;
import com.recnaile.outward.repository.IssueTransactionRepository;
import com.recnaile.outward.repository.OutwardAuditRepository;
import com.recnaile.outward.repository.OutwardRepository;
import com.recnaile.outward.observability.OutwardMetrics;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class OutwardApplicationService {

    private final OutwardRepository outwardRepository;
    private final IssueTransactionRepository issueRepository;
    private final OutwardAuditRepository auditRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final OutwardQueryService queryService;
    private final InventoryClient inventoryClient;
    private final MasterDataClient masterDataClient;
    private final OutwardMetrics metrics;
    private final ApprovalPolicy approvalPolicy;
    private final Tracer tracer;
    private final boolean employeeSelfOnly;

    public OutwardApplicationService(
            OutwardRepository outwardRepository,
            IssueTransactionRepository issueRepository,
            OutwardAuditRepository auditRepository,
            IdempotencyService idempotencyService,
            OutboxService outboxService,
            OutwardQueryService queryService,
            InventoryClient inventoryClient,
            MasterDataClient masterDataClient,
            OutwardMetrics metrics,
            ApprovalPolicy approvalPolicy,
            Tracer tracer,
            @Value("${app.authorization.employee-self-only:false}") boolean employeeSelfOnly) {
        this.outwardRepository = outwardRepository;
        this.issueRepository = issueRepository;
        this.auditRepository = auditRepository;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.queryService = queryService;
        this.inventoryClient = inventoryClient;
        this.masterDataClient = masterDataClient;
        this.metrics = metrics;
        this.approvalPolicy = approvalPolicy;
        this.tracer = tracer;
        this.employeeSelfOnly = employeeSelfOnly;
    }

    @Transactional
    public OutwardResponse create(CreateOutwardRequest request, String principal, String idempotencyKey) {
        Optional<String> replay = idempotencyService.existingOrReserve(
                principal, "POST:/api/v1/outward", idempotencyKey, request);
        if (replay.isPresent()) {
            metrics.idempotentReplay();
            return queryService.get(replay.get());
        }

        validateRequesterTarget(principal, request.recipientUserId());
        ItemResponse item = requireActiveItem(request.itemId());
        requireActiveDepartment(request.departmentId());
        if (request.projectId() != null && !request.projectId().isBlank()) {
            requireActiveProject(request.projectId());
        }

        Instant now = Instant.now();
        OutwardRequest outward = new OutwardRequest();
        outward.setOutwardId("OUT-" + UUID.randomUUID());
        outward.setItemId(request.itemId());
        outward.setQuantity(request.quantity());
        outward.setRecipientUserId(request.recipientUserId().trim());
        outward.setRecipientName(normalize(request.recipientName()));
        outward.setDepartmentId(request.departmentId().trim());
        outward.setProjectId(normalize(request.projectId()));
        outward.setPurpose(request.purpose().trim());
        outward.setLocation(normalize(request.location()));
        outward.setRequesterId(principal);
        outward.setStatus(OutwardStatus.PENDING_APPROVAL.name());
        outward.setRequestedAt(now);
        outward.setCreatedAt(now);
        outward.setUpdatedAt(now);
        outward.setAssetTracked(item.assetTracked());

        OutwardRequest saved = outwardRepository.save(outward);
        audit(saved, "OUTWARD_CREATED", principal, null, state(saved, null));
        metrics.created();

        idempotencyService.complete(principal, "POST:/api/v1/outward", idempotencyKey, request, saved.getOutwardId());
        return queryService.toResponse(saved);
    }

    @Transactional
    public OutwardResponse approve(String outwardId, String approverId, String approverRole,
                                   String idempotencyKey) {
        approvalPolicy.validate(approverRole);
        Map<String, String> command = Map.of("outwardId", outwardId);
        Optional<String> replay = idempotencyService.existingOrReserve(
                approverId, "POST:/api/v1/outward/*/approve", idempotencyKey, command);
        if (replay.isPresent()) {
            metrics.idempotentReplay();
            return queryService.get(replay.get());
        }

        OutwardRequest outward = getEntity(outwardId);
        OutwardStatus current = OutwardStatus.valueOf(outward.getStatus());
        if (current != OutwardStatus.PENDING_APPROVAL) {
            throw new com.recnaile.outward.exception.InvalidOutwardTransitionException(
                    "INVALID_OUTWARD_STATE_TRANSITION",
                    "Only PENDING_APPROVAL requests can be approved");
        }

        ItemResponse item = requireActiveItem(outward.getItemId());
        InventorySummaryResponse inventory = inventoryClient.summary(outward.getItemId());
        if (inventory.availableQuantity() < outward.getQuantity()) {
            metrics.stockRejected();
            throw new InsufficientStockException(outward.getItemId(),
                    outward.getQuantity(), inventory.availableQuantity());
        }

        Instant now = Instant.now();
        Map<String, Object> before = state(outward, null);

        outward.setApproverId(approverId);
        outward.setApproverRole(approverRole);
        outward.setApprovedAt(now);
        OutwardStateMachine.transition(outward, OutwardStatus.APPROVED, now);

        // Approval and creation of the issue transaction happen in one Mongo transaction.
        OutwardStateMachine.transition(outward, OutwardStatus.ISSUED, now);
        outward.setIssuerId(approverId);
        outward.setIssuedAt(now);
        outward.setUpdatedAt(now);

        IssueTransaction tx = new IssueTransaction(
                "ISSUE-" + UUID.randomUUID(),
                outward.getOutwardId(),
                outward.getItemId(),
                outward.getQuantity(),
                outward.getRecipientUserId(),
                outward.getRecipientName(),
                outward.getDepartmentId(),
                outward.getProjectId(),
                outward.getPurpose(),
                outward.getLocation(),
                approverId,
                outward.getApproverId(),
                now);

        try {
            outwardRepository.save(outward);
            issueRepository.save(tx);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentOutwardModificationException();
        }

        audit(outward, "OUTWARD_APPROVED_AND_ISSUED", approverId, before, state(outward, null));
        outboxService.enqueueIssued(outward, tx);
        idempotencyService.complete(approverId, "POST:/api/v1/outward/*/approve",
                idempotencyKey, command, outward.getOutwardId());
        metrics.approved();
        metrics.issued();

        return queryService.toResponse(outward);
    }

    @Transactional
    public OutwardResponse reject(String outwardId, String approverId, String approverRole,
                                   RejectOutwardRequest request) {
        OutwardRequest outward = getEntity(outwardId);
        Map<String, Object> before = state(outward, null);
        OutwardStateMachine.transition(outward, OutwardStatus.REJECTED, Instant.now());
        outward.setApproverId(approverId);
        outward.setApproverRole(approverRole);
        outward.setRejectionReason(request.reason().trim());
        outward.setUpdatedAt(Instant.now());

        try {
            outwardRepository.save(outward);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentOutwardModificationException();
        }

        audit(outward, "OUTWARD_REJECTED", approverId, before, state(outward, null));
        outboxService.enqueueRejected(outward, request.reason().trim());
        metrics.rejected();
        return queryService.toResponse(outward);
    }

    @Transactional
    public OutwardResponse cancel(String outwardId, String actorId, CancelOutwardRequest request) {
        OutwardRequest outward = getEntity(outwardId);
        if (!actorId.equals(outward.getRequesterId()) && !hasRole("PROCUREMENT", "TL", "MD")) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "ACCESS_DENIED", "Only the requester or authorized procurement roles may cancel the request");
        }
        Map<String, Object> before = state(outward, null);
        OutwardStateMachine.transition(outward, OutwardStatus.CANCELLED, Instant.now());
        outward.setUpdatedAt(Instant.now());
        outwardRepository.save(outward);
        audit(outward, "OUTWARD_CANCELLED", actorId, before, state(outward, null));
        outboxService.enqueueCancelled(outward, request == null ? null : normalize(request.reason()));
        metrics.cancelled();
        return queryService.toResponse(outward);
    }

    private OutwardRequest getEntity(String outwardId) {
        return outwardRepository.findByOutwardId(outwardId)
                .orElseThrow(() -> new OutwardNotFoundException(outwardId));
    }

    private ItemResponse requireActiveItem(String itemId) {
        ItemResponse item = masterDataClient.item(itemId);
        if (item == null || item.itemId() == null) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "ITEM_NOT_FOUND", "Item does not exist: " + itemId);
        }
        if (item.status() != null && !"ACTIVE".equalsIgnoreCase(item.status())) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "ITEM_INACTIVE", "Item is inactive: " + itemId);
        }
        return item;
    }

    private void requireActiveDepartment(String id) {
        ReferenceResponse ref = masterDataClient.department(id);
        if (ref == null || ref.id() == null) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "DEPARTMENT_NOT_FOUND", "Department does not exist: " + id);
        }
        if (ref.status() != null && !"ACTIVE".equalsIgnoreCase(ref.status())) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "DEPARTMENT_INACTIVE", "Department is inactive: " + id);
        }
    }

    private void requireActiveProject(String id) {
        ReferenceResponse ref = masterDataClient.project(id);
        if (ref == null || ref.id() == null) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "PROJECT_NOT_FOUND", "Project does not exist: " + id);
        }
        if (ref.status() != null && !"ACTIVE".equalsIgnoreCase(ref.status())) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "PROJECT_INACTIVE", "Project is inactive: " + id);
        }
    }

    private void validateRequesterTarget(String requester, String recipient) {
        if (employeeSelfOnly && hasRole("EMPLOYEE") && !requester.equals(recipient)) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "RECIPIENT_NOT_ALLOWED", "Employees may create outward requests only for themselves");
        }
    }

    private boolean hasRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (var authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            for (String role : roles) {
                if (("ROLE_" + role).equals(value)) return true;
            }
        }
        return false;
    }

    private void audit(OutwardRequest outward, String action, String actorId,
                       Map<String, Object> before, Map<String, Object> after) {
        String role = currentRole();
        String traceId = MDC.get("traceId");
        if ((traceId == null || traceId.isBlank()) && tracer != null) {
            Span span = tracer.currentSpan();
            if (span != null) traceId = span.context().traceId();
        }
        auditRepository.save(new com.recnaile.outward.domain.model.OutwardAuditRecord(
                UUID.randomUUID().toString(), "OUTWARD", outward.getOutwardId(), action,
                actorId, role, Instant.now(), MDC.get("correlationId"), traceId, before, after));
    }

    private Map<String, Object> state(OutwardRequest x, String overrideStatus) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", overrideStatus == null ? x.getStatus() : overrideStatus);
        m.put("itemId", x.getItemId());
        m.put("quantity", x.getQuantity());
        m.put("recipientUserId", x.getRecipientUserId());
        m.put("departmentId", x.getDepartmentId());
        m.put("projectId", x.getProjectId());
        return m;
    }

    private String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "SYSTEM";
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("SYSTEM");
    }

    private String normalize(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }
}
