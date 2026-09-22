package com.recnaile.indent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.indent.client.masterdata.DepartmentResponse;
import com.recnaile.indent.client.masterdata.ItemMasterResponse;
import com.recnaile.indent.client.masterdata.MasterDataClient;
import com.recnaile.indent.client.masterdata.ProjectResponse;
import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.config.IdempotencyProperties;
import com.recnaile.indent.domain.enums.ApprovalLevel;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.IdempotencyRecord;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.domain.model.IndentItem;
import com.recnaile.indent.domain.state.IndentStateMachine;
import com.recnaile.indent.dto.request.ApproveIndentRequest;
import com.recnaile.indent.dto.request.CreateIndentRequest;
import com.recnaile.indent.dto.request.RejectIndentRequest;
import com.recnaile.indent.dto.response.AutoVerifyItemResponse;
import com.recnaile.indent.dto.response.AutoVerifyResponse;
import com.recnaile.indent.dto.response.IndentDetailsResponse;
import com.recnaile.indent.dto.response.IndentItemResponse;
import com.recnaile.indent.dto.response.IndentListItemResponse;
import com.recnaile.indent.dto.response.IndentSummaryResponse;
import com.recnaile.indent.exception.ApprovalConfigurationException;
import com.recnaile.indent.exception.ConcurrentIndentModificationException;
import com.recnaile.indent.exception.IndentValidationException;
import com.recnaile.indent.repository.IndentRepository;
import com.recnaile.indent.client.masterdata.MasterDataException;

@Service
public class IndentApplicationService {

    private static final int MONEY_SCALE = 2;

    private final IndentRepository indentRepository;
    private final IndentNumberGenerator indentNumberGenerator;
    private final MasterDataClient masterDataClient;
    private final ApprovalProperties approvalProperties;
    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties idempotencyProperties;
    private final ObjectMapper objectMapper;
    private final OutboxEventService outboxEventService;

    public IndentApplicationService(
            IndentRepository indentRepository,
            IndentNumberGenerator indentNumberGenerator,
            MasterDataClient masterDataClient,
            ApprovalProperties approvalProperties,
            IdempotencyService idempotencyService,
            IdempotencyProperties idempotencyProperties,
            ObjectMapper objectMapper,
            OutboxEventService outboxEventService) {
        this.indentNumberGenerator = indentNumberGenerator;

        this.indentRepository = indentRepository;
        this.masterDataClient = masterDataClient;
        this.approvalProperties = approvalProperties;
        this.idempotencyService = idempotencyService;
        this.idempotencyProperties = idempotencyProperties;
        this.objectMapper = objectMapper;
        this.outboxEventService = outboxEventService;
    }

    public Page<IndentListItemResponse> list(
            IndentStatus status,
            String departmentName,
            Pageable pageable) {

        if (departmentName != null && departmentName.isBlank()) {
            departmentName = null;
        }

        return indentRepository
                .findAllByFilters(status, departmentName, pageable)
                .map(indent -> new IndentListItemResponse(
                        indent.getId(),
                        indent.getIndentNumber(),
                        indent.getRequesterId(),
                        indent.getDepartmentName(),
                        indent.getProjectId(),
                        indent.getIndentCreationDate(),
                        indent.getStatus(),
                        indent.getEstimatedTotalAmount()));
    }

    public IndentDetailsResponse getDetails(String indentNumber) {

        Indent indent = findIndent(indentNumber);

        List<IndentItemResponse> items =
                indent.getItems() == null
                        ? List.of()
                        : indent.getItems().stream()
                                .map(item -> new IndentItemResponse(
                                        item.getSerialNumber(),
                                        item.getItemName(),
                                        item.getQuantity(),
                                        item.getEstimatedUnitPrice(),
                                        item.getEstimatedLineAmount(),
                                        item.getRemarks(),
                                        item.getVerificationStatus(),
                                        item.getVerificationMessage(),
                                        item.getVerifiedAt()))
                                .toList();

        return new IndentDetailsResponse(
                indent.getId(),
                indent.getIndentNumber(),
                indent.getRequesterId(),
                indent.getDepartmentName(),
                indent.getProjectId(),
                indent.getReason(),
                indent.getRemarks(),
                indent.getIndentCreationDate(),
                indent.getStatus(),
                items,
                indent.getEstimatedTotalAmount());
    }

    @Transactional
    public IndentSummaryResponse createAndSubmit(
            CreateIndentRequest request,
            String requesterId) {

        if (requesterId == null || requesterId.isBlank()) {
            throw new IndentValidationException(
                    "REQUESTER_ID_REQUIRED",
                    "Authenticated requester identity is required");
        }

        DepartmentResponse department =
                getDepartment(request.departmentName());

        if (!department.isActive()) {
            throw new IndentValidationException(
                    "DEPARTMENT_INACTIVE",
                    "Department is not active: "
                            + request.departmentName());
        }

        if (request.projectId() != null
                && !request.projectId().isBlank()) {

            ProjectResponse project =
                    getProject(request.projectId());

            if (!project.isActive()) {
                throw new IndentValidationException(
                        "PROJECT_INACTIVE",
                        "Project is not active: "
                                + request.projectId());
            }
        }

        validateDuplicateItems(request.items());

        List<IndentItem> indentItems =
                new ArrayList<>();

        BigDecimal total =
                BigDecimal.ZERO;

        for (CreateIndentRequest.ItemRequest itemRequest
                : request.items()) {

            /*
             * CREATE INDENT is a request operation.
             *
             * No Item Master validation is performed here.
             * No Inventory validation is performed here.
             *
             * Procurement is responsible for validating the requested
             * item and checking its inventory availability during the
             * verification / auto-verify process.
             */
            validateQuantity(
                    itemRequest.quantity(),
                    itemRequest.serialNumber());

            BigDecimal unitPrice =
                    resolveUnitPrice(itemRequest);

            BigDecimal lineTotal =
                    unitPrice
                            .multiply(
                                    BigDecimal.valueOf(
                                            itemRequest.quantity()))
                            .setScale(
                                    MONEY_SCALE,
                                    RoundingMode.HALF_UP);

            IndentItem indentItem =
                    new IndentItem(
                            itemRequest.serialNumber(),
                            itemRequest.itemName(),
                            itemRequest.quantity(),
                            unitPrice,
                            lineTotal,
                            blankToNull(itemRequest.remarks())
                    );

            indentItems.add(indentItem);

            total = total.add(lineTotal);
        }

        total = total.setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP);

        Instant now = Instant.now();

        Indent indent = new Indent();

        LocalDate creationDate =
                java.time.LocalDate.ofInstant(
                        now,
                        java.time.ZoneOffset.systemDefault()
                                .getRules()
                                .getOffset(now));

        indent.setIndentNumber(
                indentNumberGenerator.nextIndentNumber(
                        creationDate));

        indent.setIndentCreationDate(now);
        indent.setDepartmentName(request.departmentName());
        indent.setRequesterId(requesterId);
        indent.setDepartmentId(department.id());
        indent.setProjectId(
                blankToNull(request.projectId()));
        indent.setReason(request.reason().trim());
        indent.setItems(indentItems);
        indent.setEstimatedTotalAmount(total);

        /*
         * Creation always submits the indent.
         * Inventory availability has no effect on this transition.
         */
        indent.setStatus(IndentStatus.DRAFT);
        indent.setCreatedAt(now);
        indent.setUpdatedAt(now);

        IndentStateMachine.validateTransition(
                indent.getStatus(),
                IndentStatus.SUBMITTED);

        indent.setStatus(
                IndentStatus.SUBMITTED);

        indent.setSubmittedAt(now);
        indent.setUpdatedAt(now);

        Indent saved =
                indentRepository.save(indent);

        IndentSubmittedEvent eventPayload =
                new IndentSubmittedEvent(
                        saved.getId(),
                        saved.getDepartmentId(),
                        saved.getProjectId(),
                        saved.getRequesterId(),
                        saved.getItems().stream()
                                .map(item -> new SubmittedItem(
                                        item.getSerialNumber(),
                                        item.getQuantity()))
                                .toList(),
                        saved.getEstimatedTotalAmount());

        outboxEventService.create(
                "INDENT_SUBMITTED",
                "1",
                saved.getSubmittedAt(),
                saved.getId(),
                null,
                null,
                requesterId,
                "EMPLOYEE",
                "procurement.indent.events",
                eventPayload);

        return new IndentSummaryResponse(
                saved.getId(),
                saved.getIndentNumber(),
                saved.getRequesterId(),
                saved.getDepartmentName(),
                saved.getProjectId(),
                saved.getReason(),
                saved.getRemarks(),
                saved.getIndentCreationDate(),
                saved.getStatus(),
                saved.getEstimatedTotalAmount());
    }

    @Transactional
    public IndentSummaryResponse verify(
            String indentNumber,
            String verifierId,
            String remarks) {

        if (verifierId == null || verifierId.isBlank()) {
            throw new IndentValidationException(
                    "VERIFIER_ID_REQUIRED",
                    "Authenticated verifier identity is required");
        }

        Indent indent = findIndent(indentNumber);

        IndentStateMachine.validateTransition(
                indent.getStatus(), IndentStatus.UNDER_VERIFICATION);
        indent.setStatus(IndentStatus.UNDER_VERIFICATION);

        IndentStateMachine.validateTransition(
                indent.getStatus(), IndentStatus.VERIFIED);
        indent.setStatus(IndentStatus.VERIFIED);

        Instant now = Instant.now();
        indent.setVerifierId(verifierId);
        indent.setVerifiedAt(now);
        indent.setVerificationRemarks(blankToNull(remarks));

        ApprovalLevel approvalLevel =
                determineApprovalLevel(indent.getEstimatedTotalAmount());
        indent.setApprovalLevel(approvalLevel);

        IndentStateMachine.validateTransition(
                indent.getStatus(), IndentStatus.PENDING_APPROVAL);
        indent.setStatus(IndentStatus.PENDING_APPROVAL);
        indent.setApprovalDueAt(
                now.plus(approvalProperties.getSlaDuration()));
        indent.setUpdatedAt(now);

        Indent saved;
        try {
            saved = indentRepository.save(indent);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentIndentModificationException(
                    "Indent was modified by another request. Please refresh and try again.", ex);
        }

        outboxEventService.create(
                "INDENT_VERIFIED", "1", saved.getVerifiedAt(), saved.getId(),
                null, null, verifierId, "PROCUREMENT", "procurement.indent.events",
                new IndentVerifiedEvent(
                        saved.getId(), verifierId, saved.getEstimatedTotalAmount()));

        return summary(saved);
    }

    @Transactional
    public IndentSummaryResponse approve(
            String indentNumber,
            String approverId,
            String approvalRole,
            ApproveIndentRequest request,
            String idempotencyKey) {

        if (approverId == null || approverId.isBlank()) {
            throw new IndentValidationException(
                    "APPROVER_ID_REQUIRED",
                    "Authenticated approver identity is required");
        }
        if (approvalRole == null || approvalRole.isBlank()) {
            throw new IndentValidationException(
                    "APPROVAL_ROLE_REQUIRED",
                    "Authenticated approval role is required");
        }

        String operation = "APPROVE";
        String requestHash = idempotencyService.requestHash(request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyService.find(
                    approverId, operation, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                idempotencyService.assertSameRequest(record, requestHash);
                return deserializeResponse(record.getResponseBody());
            }
        }

        Indent indent = findIndent(indentNumber);

        if (indent.getStatus() != IndentStatus.PENDING_APPROVAL) {
            throw new IndentValidationException(
                    "APPROVAL_NOT_ALLOWED",
                    "Indent is not pending approval: " + indentNumber);
        }
        if (indent.getApprovalLevel() == null) {
            throw new IndentValidationException(
                    "APPROVAL_LEVEL_MISSING",
                    "Approval level has not been assigned: " + indentNumber);
        }

        ApprovalLevel requiredLevel =
                "MD".equalsIgnoreCase(approvalRole) ? ApprovalLevel.MD
                        : "TL".equalsIgnoreCase(approvalRole) ? ApprovalLevel.TL
                        : null;

        if (requiredLevel == null || indent.getApprovalLevel() != requiredLevel) {
            throw new IndentValidationException(
                    "APPROVAL_ROLE_NOT_ALLOWED",
                    "Authenticated role is not allowed to approve this indent");
        }

        Instant now = Instant.now();
        indent.setApproverId(approverId);
        indent.setApprovedAt(now);
        indent.setApprovalRemarks(request == null ? null : blankToNull(request.remarks()));

        IndentStateMachine.validateTransition(
                indent.getStatus(), IndentStatus.APPROVED);
        indent.setStatus(IndentStatus.APPROVED);
        IndentStateMachine.validateTransition(
                indent.getStatus(), IndentStatus.READY_FOR_PO);
        indent.setStatus(IndentStatus.READY_FOR_PO);
        indent.setUpdatedAt(now);

        Indent saved;
        try {
            saved = indentRepository.save(indent);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentIndentModificationException(
                    "Indent was modified by another request. Please refresh and try again.", ex);
        }

        outboxEventService.create(
                "INDENT_APPROVED", "1", saved.getApprovedAt(), saved.getId(),
                null, null, approverId, approvalRole.toUpperCase(), "procurement.indent.events",
                new IndentApprovedEvent(
                        saved.getId(), approverId, saved.getEstimatedTotalAmount(), saved.getProjectId()));

        IndentSummaryResponse response = summary(saved);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(
                    approverId, operation, idempotencyKey, requestHash,
                    saved.getId(), response, 200, idempotencyProperties.ttl());
        }
        return response;
    }

    @Transactional
    public IndentSummaryResponse reject(
            String indentNumber,
            String actorId,
            RejectIndentRequest request,
            String idempotencyKey) {

        if (actorId == null || actorId.isBlank()) {
            throw new IndentValidationException(
                    "ACTOR_ID_REQUIRED",
                    "Authenticated actor identity is required");
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new IndentValidationException(
                    "REJECTION_REASON_REQUIRED",
                    "Rejection reason is mandatory");
        }

        String operation = "REJECT";
        String requestHash = idempotencyService.requestHash(request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyService.find(actorId, operation, idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();
                idempotencyService.assertSameRequest(record, requestHash);
                return deserializeResponse(record.getResponseBody());
            }
        }

        Indent indent = findIndent(indentNumber);

        if (indent.getStatus() != IndentStatus.PENDING_APPROVAL
                && indent.getStatus() != IndentStatus.UNDER_VERIFICATION) {
            throw new IndentValidationException(
                    "REJECTION_NOT_ALLOWED",
                    "Indent cannot be rejected from status: " + indent.getStatus());
        }

        Instant now = Instant.now();
        IndentStateMachine.validateTransition(
                indent.getStatus(), IndentStatus.REJECTED);
        indent.setStatus(IndentStatus.REJECTED);
        indent.setRejectionReason(request.reason().trim());
        indent.setRejectionActorId(actorId);
        indent.setRejectedAt(now);
        indent.setUpdatedAt(now);

        Indent saved;
        try {
            saved = indentRepository.save(indent);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentIndentModificationException(
                    "Indent was modified by another request. Please refresh and try again.", ex);
        }

        outboxEventService.create(
                "INDENT_REJECTED", "1", saved.getRejectedAt(), saved.getId(),
                null, null, actorId, null, "procurement.indent.events",
                new IndentRejectedEvent(
                        saved.getId(), actorId, saved.getRejectionReason(), saved.getRejectedAt()));

        IndentSummaryResponse response = summary(saved);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(
                    actorId, operation, idempotencyKey, requestHash,
                    saved.getId(), response, 200, idempotencyProperties.ttl());
        }
        return response;
    }

    /**
     * Auto-verifies every indent item against the Item Master list in
     * Master Data Service.
     *
     * This is a read/check operation from the indent workflow perspective.
     * It does not change the main indent workflow status. It does, however,
     * persist the verification result on each individual indent item.
     *
     * Rules:
     * - matching active Item Master record -> AVAILABLE
     * - no matching Item Master record -> NOT_AVAILABLE
     * - matching inactive Item Master record -> NOT_AVAILABLE
     */
    public AutoVerifyResponse autoVerifyInventory(
            String indentNumber) {

        Indent indent = findIndent(indentNumber);

        List<ItemMasterResponse> masterItems =
                masterDataClient.getAllItems();

        List<AutoVerifyItemResponse> results =
                new ArrayList<>();

        for (IndentItem item : indent.getItems()) {

            ItemMasterResponse masterItem =
                    findMasterItemByName(masterItems, item.getItemName());

            String status;
            String message;

            if (masterItem != null && masterItem.isActive()) {
                status = "AVAILABLE";
                message = "Item is available in the inventory.";
            } else {
                status = "NOT_AVAILABLE";
                message = "Item is not available in the inventory.";
            }

            item.setVerificationStatus(status);
            item.setVerificationMessage(message);
            item.setVerifiedAt(Instant.now());

            results.add(new AutoVerifyItemResponse(
                    item.getSerialNumber(),
                    item.getItemName(),
                    item.getQuantity(),
                    null,
                    status,
                    message));
        }

        indent.setUpdatedAt(Instant.now());
        indentRepository.save(indent);

        return new AutoVerifyResponse(
                indent.getIndentNumber(),
                results);
    }

    private ItemMasterResponse findMasterItemByName(
            List<ItemMasterResponse> masterItems,
            String itemName) {

        if (masterItems == null || itemName == null) {
            return null;
        }

        return masterItems.stream()
                .filter(item -> item != null
                        && item.name() != null
                        && item.name().trim().equalsIgnoreCase(itemName.trim()))
                .findFirst()
                .orElse(null);
    }

    private IndentSummaryResponse summary(Indent saved) {
        return new IndentSummaryResponse(
                saved.getId(),
                saved.getIndentNumber(),
                saved.getRequesterId(),
                saved.getDepartmentName(),
                saved.getProjectId(),
                saved.getReason(),
                saved.getRemarks(),
                saved.getIndentCreationDate(),
                saved.getStatus(),
                saved.getEstimatedTotalAmount());
    }

    private Indent findIndent(String indentNumber) {
        return indentRepository.findByIndentNumber(indentNumber)
                .orElseThrow(() -> new IndentValidationException(
                        "INDENT_NOT_FOUND",
                        "Indent was not found: " + indentNumber));
    }

    private ApprovalLevel determineApprovalLevel(BigDecimal amount) {
        BigDecimal threshold = approvalProperties.getThreshold();
        if (threshold == null || threshold.signum() < 0) {
            throw new ApprovalConfigurationException(
                    "Approval threshold must be configured and non-negative");
        }
        if (amount == null || amount.signum() < 0) {
            throw new ApprovalConfigurationException(
                    "Indent amount must be present and non-negative");
        }
        return amount.compareTo(threshold) < 0 ? ApprovalLevel.TL : ApprovalLevel.MD;
    }

    private DepartmentResponse getDepartment(String departmentName) {
        try {
            return masterDataClient.getAllDepartments().stream()
                    .filter(department -> department != null
                            && department.departmentName() != null
                            && department.departmentName().trim()
                                    .equalsIgnoreCase(departmentName.trim()))
                    .findFirst()
                    .orElseThrow(() -> new MasterDataException(
                            MasterDataException.Reason.NOT_FOUND,
                            departmentName,
                            "Department was not found: " + departmentName));
        } catch (MasterDataException ex) {
            throw mapMasterDataException("DEPARTMENT", ex);
        }
    }

    private ProjectResponse getProject(String projectId) {
        try {
            return masterDataClient.getProject(projectId);
        } catch (MasterDataException ex) {
            throw mapMasterDataException("PROJECT", ex);
        }
    }

    private IndentValidationException mapMasterDataException(
            String type, MasterDataException ex) {
        return switch (ex.getReason()) {
            case NOT_FOUND -> new IndentValidationException(
                    type + "_NOT_FOUND", type + " was not found: " + ex.getReferenceId());
            case INACTIVE -> new IndentValidationException(
                    type + "_INACTIVE", type + " is inactive: " + ex.getReferenceId());
            case INVALID_RESPONSE, UNAVAILABLE -> new IndentValidationException(
                    "MASTER_DATA_UNAVAILABLE",
                    "Unable to validate " + type.toLowerCase() + " with Master Data service");
        };
    }

    private void validateDuplicateItems(
            List<CreateIndentRequest.ItemRequest> items) {
        Set<String> itemNames = new HashSet<>();
        for (CreateIndentRequest.ItemRequest item : items) {
            if (!itemNames.add(item.itemName().trim().toLowerCase())) {
                throw new IndentValidationException(
                        "DUPLICATE_ITEM",
                        "Duplicate item in indent: " + item.itemName());
            }
        }
    }

    private void validateQuantity(Integer quantity, String serialNumber) {
        if (quantity == null || quantity < 1) {
            throw new IndentValidationException(
                    "INVALID_QUANTITY",
                    "Quantity must be greater than zero for item: " + serialNumber);
        }
    }

    private BigDecimal resolveUnitPrice(CreateIndentRequest.ItemRequest request) {
        BigDecimal unitPrice = request.estimatedUnitPrice();
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IndentValidationException(
                    "UNIT_PRICE_REQUIRED",
                    "A valid estimated unit price is required for item: " + request.itemName());
        }
        return unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private IndentSummaryResponse deserializeResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, IndentSummaryResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to deserialize stored idempotency response", ex);
        }
    }

    private record SubmittedItem(String serialNumber, Integer quantity) {}

    private record IndentSubmittedEvent(
            String indentId,
            String departmentId,
            String projectId,
            String requesterId,
            List<SubmittedItem> items,
            BigDecimal amount) {}

    private record IndentVerifiedEvent(
            String indentId,
            String verifierId,
            BigDecimal amount) {}

    private record IndentApprovedEvent(
            String indentId,
            String approverId,
            BigDecimal amount,
            String projectId) {}

    private record IndentRejectedEvent(
            String indentId,
            String rejectorId,
            String reason,
            Instant timestamp) {}

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}