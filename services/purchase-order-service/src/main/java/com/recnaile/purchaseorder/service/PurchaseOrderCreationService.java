package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.enums.PurchaseType;
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

    private final PurchaseOrderRepository repository;
    private final PurchaseOrderNumberGenerator numberGenerator;
    private final PurchaseOrderCommercialCalculator commercialCalculator;

    public PurchaseOrderCreationService(PurchaseOrderRepository repository,
                                        PurchaseOrderNumberGenerator numberGenerator,
                                        PurchaseOrderCommercialCalculator commercialCalculator) {
        this.repository = repository;
        this.numberGenerator = numberGenerator;
        this.commercialCalculator = commercialCalculator;
    }

    /**
     * Creates a PO from an indent that has already reached READY_FOR_PO in Indent Service.
     *
     * This service deliberately accepts a validated snapshot rather than reading the
     * Indent Service database. Cross-service persistence access is not allowed.
     * The synchronous REST/API adapter or event consumer will supply the snapshot
     * from the documented Indent Service contract in a later phase.
     */
    @Transactional
    public PurchaseOrder createFromApprovedIndent(
            CreatePurchaseOrderFromApprovedIndentCommand command,
            String actorId) {

        validateCommand(command);

        if (!"READY_FOR_PO".equalsIgnoreCase(command.indentStatus().trim())) {
            throw new BusinessRuleException(
                    "INDENT_NOT_READY_FOR_PO",
                    "Only an indent in READY_FOR_PO status can be converted to a purchase order: " + command.indentId());
        }

        if (repository.findBySourceIndentIdsIn(List.of(command.indentId())).stream()
                .anyMatch(po -> po.getSourceIndentIds().contains(command.indentId()))) {
            throw new BusinessRuleException(
                    "INDENT_ALREADY_CONVERTED",
                    "The approved indent is already linked to a purchase order: " + command.indentId());
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoDate(LocalDate.now());
        po.setPoNumber(numberGenerator.nextNumber(po.getPoDate()));
        po.setSourceIndentIds(List.of(command.indentId()));
        po.setVendorId(command.vendorId().trim());
        po.setProjectId(blankToNull(command.projectId()));
        po.setDepartmentId(command.departmentId().trim());
        po.setPurchaseType(command.projectId() == null || command.projectId().isBlank()
                ? PurchaseType.STANDARD
                : PurchaseType.STANDARD);
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
        commercialCalculator.calculate(po);

        return repository.save(po);
    }

    private void validateCommand(CreatePurchaseOrderFromApprovedIndentCommand command) {
        if (command == null) {
            throw new BusinessRuleException("INVALID_PO_CREATION_REQUEST", "Purchase order creation request is required");
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
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
