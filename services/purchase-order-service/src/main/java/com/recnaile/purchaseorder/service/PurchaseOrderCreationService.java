package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.enums.PurchaseType;
import com.recnaile.purchaseorder.domain.model.IdempotencyRecord;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderLine;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderCreationService {

    private static final String OPERATION = "CREATE_FROM_APPROVED_INDENT";

    private final PurchaseOrderRepository repository;
    private final PurchaseOrderNumberGenerator numberGenerator;
    private final PurchaseOrderCommercialCalculator commercialCalculator;
    private final PurchaseOrderIdempotencyService idempotencyService;
    private final PurchaseOrderOutboxService outboxService;

    public PurchaseOrderCreationService(PurchaseOrderRepository repository,
                                        PurchaseOrderNumberGenerator numberGenerator,
                                        PurchaseOrderCommercialCalculator commercialCalculator,
                                        PurchaseOrderIdempotencyService idempotencyService,
                                        PurchaseOrderOutboxService outboxService) {
        this.repository = repository;
        this.numberGenerator = numberGenerator;
        this.commercialCalculator = commercialCalculator;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
    }

    @Transactional
    public PurchaseOrder createFromApprovedIndent(
            CreatePurchaseOrderFromApprovedIndentCommand command,
            String actorId,
            String idempotencyKey) {

        validateCommand(command);

        String requestHash = idempotencyService.requestHash(
                OPERATION,
                command == null ? null : command.indentId(),
                command);

        PurchaseOrderIdempotencyService.Reservation reservation =
                idempotencyService.reserve(actorId, idempotencyKey, OPERATION, requestHash);

        IdempotencyRecord record = reservation.record();

        if (!reservation.newlyReserved()) {
            idempotencyService.assertSameRequest(record, OPERATION, requestHash);
            return replay(record);
        }

        try {
            PurchaseOrder po = create(command, actorId);
            PurchaseOrder saved = repository.save(po);
            outboxService.enqueueCreated(saved, actorId);
            idempotencyService.complete(record, saved.getPoId(), 201);
            return saved;
        } catch (RuntimeException ex) {
            idempotencyService.release(record);
            throw ex;
        }
    }

    private PurchaseOrder replay(IdempotencyRecord record) {
        if (record.getResourceId() == null || record.getResourceId().isBlank()) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_RESOURCE_NOT_FOUND",
                    "The previous idempotent operation has no stored purchase order reference");
        }
        return repository.findById(record.getResourceId())
                .orElseThrow(() -> new BusinessRuleException(
                        "IDEMPOTENCY_RESOURCE_NOT_FOUND",
                        "The purchase order created by the previous request no longer exists"));
    }

    private PurchaseOrder create(CreatePurchaseOrderFromApprovedIndentCommand command,
                                 String actorId) {
        if (!"READY_FOR_PO".equalsIgnoreCase(command.indentStatus().trim())) {
            throw new BusinessRuleException(
                    "INDENT_NOT_READY_FOR_PO",
                    "Only an indent in READY_FOR_PO status can be converted to a purchase order: "
                            + command.indentId());
        }

        if (repository.findBySourceIndentIdsIn(List.of(command.indentId())).stream()
                .anyMatch(po -> po.getSourceIndentIds().contains(command.indentId()))) {
            throw new BusinessRuleException(
                    "INDENT_ALREADY_CONVERTED",
                    "The approved indent is already linked to a purchase order: "
                            + command.indentId());
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoDate(LocalDate.now());
        po.setPoNumber(numberGenerator.nextNumber(po.getPoDate()));
        po.setSourceIndentIds(List.of(command.indentId()));
        po.setVendorId(command.vendorId().trim());
        po.setProjectId(blankToNull(command.projectId()));
        po.setDepartmentId(command.departmentId().trim());
        po.setPurchaseType(PurchaseType.STANDARD);
        po.setExpectedDeliveryDate(command.expectedDeliveryDate());
        po.setRemarks(blankToNull(command.remarks()));
        po.setCreatedBy(blankToNull(actorId));
        po.setUpdatedBy(blankToNull(actorId));

        List<PurchaseOrderLine> lines = new ArrayList<>();
        for (CreatePurchaseOrderFromApprovedIndentCommand.ApprovedIndentLine source : command.items()) {
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setItemId(source.itemId().trim());
            line.setItemName(blankToNull(source.itemName()));
            line.setOrderedQuantity(source.quantity());
            line.setReceivedQuantity(BigDecimal.ZERO);
            line.setUnitPrice(source.unitPrice());
            line.setGstRate(source.gstRate() == null ? BigDecimal.ZERO : source.gstRate());
            lines.add(line);
        }

        po.setLines(lines);
        po.transitionStatus(PurchaseOrderStatus.DRAFT);
        commercialCalculator.calculate(po);
        return po;
    }

    private void validateCommand(CreatePurchaseOrderFromApprovedIndentCommand command) {
        if (command == null) {
            throw new BusinessRuleException(
                    "INVALID_PO_CREATION_REQUEST",
                    "Purchase order creation request is required");
        }
        if (command.indentId() == null || command.indentId().isBlank()) {
            throw new BusinessRuleException("INDENT_ID_REQUIRED", "Approved indent ID is required");
        }
        if (command.indentStatus() == null || command.indentStatus().isBlank()) {
            throw new BusinessRuleException("INDENT_STATUS_REQUIRED", "Approved indent status is required");
        }
        if (command.vendorId() == null || command.vendorId().isBlank()) {
            throw new BusinessRuleException("VENDOR_ID_REQUIRED", "Vendor ID is required");
        }
        if (command.departmentId() == null || command.departmentId().isBlank()) {
            throw new BusinessRuleException("DEPARTMENT_ID_REQUIRED", "Department ID is required");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new BusinessRuleException("PO_ITEMS_REQUIRED", "At least one PO line is required");
        }
        if (!"READY_FOR_PO".equalsIgnoreCase(command.indentStatus().trim())) {
            throw new BusinessRuleException(
                    "INDENT_NOT_READY_FOR_PO",
                    "Only an indent in READY_FOR_PO status can be converted to a purchase order: "
                            + command.indentId());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
