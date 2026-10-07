package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderRequest;
import com.recnaile.purchaseorder.client.indent.IndentClient;
import com.recnaile.purchaseorder.client.indent.IndentDetails;
import com.recnaile.purchaseorder.client.masterdata.MasterDataReferenceClient;
import com.recnaile.purchaseorder.client.masterdata.MasterDataVendorClient;
import com.recnaile.purchaseorder.client.masterdata.VendorResponse;
import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.enums.PurchaseType;
import com.recnaile.purchaseorder.domain.model.ApprovedIndentSnapshot;
import com.recnaile.purchaseorder.domain.model.IdempotencyRecord;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderLine;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.observability.PurchaseOrderMetrics;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderCreationService {

    private static final String FROM_APPROVED_INDENT = "CREATE_FROM_APPROVED_INDENT";
    private static final String MANUAL_CREATE = "CREATE_MANUAL_PO";

    private final PurchaseOrderRepository repository;
    private final PurchaseOrderNumberGenerator numberGenerator;
    private final PurchaseOrderCommercialCalculator commercialCalculator;
    private final PurchaseOrderIdempotencyService idempotencyService;
    private final PurchaseOrderOutboxService outboxService;
    private final MasterDataVendorClient masterDataVendorClient;
    private final MasterDataReferenceClient masterDataReferenceClient;
    private final IndentClient indentClient;
    private final PurchaseOrderMetrics metrics;

    public PurchaseOrderCreationService(PurchaseOrderRepository repository,
                                        PurchaseOrderNumberGenerator numberGenerator,
                                        PurchaseOrderCommercialCalculator commercialCalculator,
                                        PurchaseOrderIdempotencyService idempotencyService,
                                        PurchaseOrderOutboxService outboxService,
                                        MasterDataVendorClient masterDataVendorClient,
                                        MasterDataReferenceClient masterDataReferenceClient,
                                        IndentClient indentClient,
                                        PurchaseOrderMetrics metrics) {
        this.repository = repository;
        this.numberGenerator = numberGenerator;
        this.commercialCalculator = commercialCalculator;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.masterDataVendorClient = masterDataVendorClient;
        this.masterDataReferenceClient = masterDataReferenceClient;
        this.indentClient = indentClient;
        this.metrics = metrics;
    }

    @Transactional
    @Observed(name = "purchase.order.command")
    public PurchaseOrder createFromApprovedIndent(
            CreatePurchaseOrderFromApprovedIndentCommand command,
            String actorId,
            String idempotencyKey) {

        validateApprovedIndentCommand(command);
        String indentNumber = command.indentNumber().trim();
        String requestHash = null;
        PurchaseOrderIdempotencyService.Reservation reservation = null;
        IdempotencyRecord record = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = idempotencyService.requestHash(FROM_APPROVED_INDENT, indentNumber, command);
            reservation = idempotencyService.reserve(actorId, idempotencyKey, FROM_APPROVED_INDENT, requestHash);
            record = reservation.record();

            if (!reservation.newlyReserved()) {
                idempotencyService.assertSameRequest(record, FROM_APPROVED_INDENT, requestHash);
                return replay(record);
            }
        }

        try {
            // The client supplies only the indent number. All business data, especially status,
            // is read from the authoritative Indent Service.
            IndentDetails indent = indentClient.getIndent(indentNumber);
            validateApprovedIndent(indent, indentNumber);

            // An indent may be converted to a purchase order only once.
            // The Mongo unique index is the final safeguard; this pre-check gives
            // callers a deterministic business error before attempting the write.
            List<PurchaseOrder> existingPurchaseOrders =
                    repository.findCurrentBySourceIndentIdsIn(List.of(indentNumber));
            if (!existingPurchaseOrders.isEmpty()) {
                throw new BusinessRuleException(
                        "INDENT_ALREADY_CONVERTED",
                        "Indent has already been converted to a purchase order: " + indentNumber);
            }

            VendorResponse vendor = masterDataVendorClient.requireActiveVendorByName(command.vendorName().trim());
            var department = masterDataReferenceClient.requireActiveDepartmentByName(indent.departmentName().trim());
            // Approved Indent is the source of truth for item data during PO conversion.
            // Item Master validation is intentionally not repeated here.
            PurchaseOrder po = createFromIndentData(command, indent, vendor, department.id(), actorId);
            PurchaseOrder saved = repository.save(po);
            outboxService.enqueueCreated(saved, actorId);
            if (record != null) {
                idempotencyService.complete(record, saved.getPoId(), 201);
            }
            metrics.recordCreated();
            return saved;
        } catch (RuntimeException ex) {
            if (record != null) {
                idempotencyService.release(record);
            }
            throw ex;
        }
    }

    @Transactional
    @Observed(name = "purchase.order.command")
    public PurchaseOrder createManual(
            CreatePurchaseOrderRequest command,
            String actorId,
            String idempotencyKey) {

        validateManualCommand(command);
        String requestHash = null;
        PurchaseOrderIdempotencyService.Reservation reservation = null;
        IdempotencyRecord record = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = idempotencyService.requestHash(MANUAL_CREATE, null, command);
            reservation = idempotencyService.reserve(actorId, idempotencyKey, MANUAL_CREATE, requestHash);
            record = reservation.record();

            if (!reservation.newlyReserved()) {
                idempotencyService.assertSameRequest(record, MANUAL_CREATE, requestHash);
                return replay(record);
            }
        }

        try {
            VendorResponse vendor = masterDataVendorClient.requireActiveVendorByName(command.vendorName().trim());
            var department = masterDataReferenceClient.requireActiveDepartmentByName(command.departmentName().trim());
            validateItemsAgainstMasterData(command.items());
            PurchaseOrder po = new PurchaseOrder();
            populateCommonFields(po, vendor, department.id(), command.projectId(), command.departmentName(),
                    command.purchaseType(), command.expectedDeliveryDate(), actorId);
            po.setPoId(numberGenerator.nextNumber());
            po.setIndentStatus(null);
            po.setSourceIndentIds(List.of());
            po.setIndentNumber(null);
            po.setLines(toLines(command.items()));
            po.transitionStatus(PurchaseOrderStatus.DRAFT);
            commercialCalculator.calculate(po);

            PurchaseOrder saved = repository.save(po);
            outboxService.enqueueCreated(saved, actorId);
            if (record != null) {
                idempotencyService.complete(record, saved.getPoId(), 201);
            }
            metrics.recordCreated();
            return saved;
        } catch (RuntimeException ex) {
            if (record != null) {
                idempotencyService.release(record);
            }
            throw ex;
        }
    }

    private PurchaseOrder createFromIndentData(CreatePurchaseOrderFromApprovedIndentCommand command,
                                                 IndentDetails indent,
                                                 VendorResponse vendor,
                                                 String departmentId,
                                                 String actorId) {
        PurchaseOrder po = new PurchaseOrder();
        populateCommonFields(po, vendor, departmentId, indent.projectId(), indent.departmentName(),
                command.purchaseType(), command.expectedDeliveryDate(), actorId);
        po.setPoId(numberGenerator.nextNumber());
        po.setIndentNumber(indent.indentNumber());
        po.setIndentStatus(indent.status());
        po.setIndentDetails(toIndentSnapshot(indent));
        po.setSourceIndentIds(List.of(indent.indentNumber()));
        po.setLines(toLines(indent.items()));
        po.transitionStatus(PurchaseOrderStatus.DRAFT);
        commercialCalculator.calculate(po);
        return po;
    }

    private ApprovedIndentSnapshot toIndentSnapshot(IndentDetails indent) {
        ApprovedIndentSnapshot snapshot = new ApprovedIndentSnapshot();
        snapshot.setRequesterId(indent.requesterId());
        snapshot.setDepartmentId(indent.departmentId());
        snapshot.setDepartmentName(indent.departmentName());
        snapshot.setProjectId(indent.projectId());
        snapshot.setReason(indent.reason());
        snapshot.setEstimatedTotalAmount(indent.estimatedTotalAmount());
        snapshot.setStatus(indent.status());
        snapshot.setApprovalLevel(indent.approvalLevel());
        snapshot.setVerifierId(indent.verifierId());
        snapshot.setVerifiedAt(indent.verifiedAt());
        snapshot.setVerificationRemarks(indent.verificationRemarks());
        snapshot.setApproverId(indent.approverId());
        snapshot.setApprovedAt(indent.approvedAt());
        snapshot.setApprovalRemarks(indent.approvalRemarks());

        List<ApprovedIndentSnapshot.Item> items = new ArrayList<>();
        if (indent.items() != null) {
            for (IndentDetails.Item source : indent.items()) {
                ApprovedIndentSnapshot.Item item = new ApprovedIndentSnapshot.Item();
                item.setItemId(source.itemId());
                item.setItemName(source.itemName());
                item.setQuantity(source.quantity());
                item.setEstimatedUnitPrice(source.unitPrice());
                item.setEstimatedLineAmount(source.estimatedLineAmount());
                item.setRemarks(source.remarks());
                items.add(item);
            }
        }
        snapshot.setItems(items);
        return snapshot;
    }

    private void populateCommonFields(PurchaseOrder po,
                                      VendorResponse vendor,
                                      String departmentId,
                                      String projectId,
                                      String departmentName,
                                      PurchaseType purchaseType,
                                      LocalDate expectedDeliveryDate,
                                      String actorId) {
        po.setPoDate(LocalDate.now());
        po.setVendorId(vendor.id());
        po.setVendorName(vendor.name());
        po.setProjectId(blankToNull(projectId));
        po.setDepartmentId(blankToNull(departmentId));
        po.setDepartmentName(departmentName.trim());
        po.setPurchaseType(purchaseType);
        po.setExpectedDeliveryDate(expectedDeliveryDate);
        po.setCreatedBy(blankToNull(actorId));
        po.setUpdatedBy(blankToNull(actorId));
    }

    private List<PurchaseOrderLine> toLines(List<?> sourceItems) {
        List<PurchaseOrderLine> lines = new ArrayList<>();
        for (Object source : sourceItems) {
            String itemId;
            String itemName;
            BigDecimal quantity;
            BigDecimal unitPrice;
            BigDecimal gstRate;

            if (source instanceof CreatePurchaseOrderRequest.Item item) {
                itemId = item.itemId();
                itemName = item.itemName();
                quantity = item.quantity();
                unitPrice = item.unitPrice();
                gstRate = item.gstRate();
            } else if (source instanceof IndentDetails.Item item) {
                itemId = item.itemId();
                itemName = item.itemName();
                quantity = item.quantity();
                unitPrice = item.unitPrice();
                gstRate = item.gstRate() == null ? BigDecimal.ZERO : item.gstRate();
            } else {
                throw new BusinessRuleException("PO_ITEMS_INVALID", "Unsupported PO item data");
            }

            if (isBlank(itemId) || isBlank(itemName) || quantity == null || quantity.signum() <= 0
                    || unitPrice == null || unitPrice.signum() < 0 || gstRate == null || gstRate.signum() < 0) {
                throw new BusinessRuleException("PO_ITEMS_INVALID", "PO item data is incomplete or invalid");
            }

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setItemId(itemId.trim());
            line.setItemName(itemName.trim());
            line.setOrderedQuantity(quantity);
            line.setReceivedQuantity(BigDecimal.ZERO);
            line.setUnitPrice(unitPrice);
            line.setGstRate(gstRate);
            lines.add(line);
        }
        return lines;
    }

    private void validateItemsAgainstMasterData(List<?> sourceItems) {
        for (Object source : sourceItems) {
            String itemId;
            String itemName;
            if (source instanceof CreatePurchaseOrderRequest.Item item) {
                itemId = item.itemId();
                itemName = item.itemName();
            } else if (source instanceof IndentDetails.Item item) {
                itemId = item.itemId();
                itemName = item.itemName();
            } else {
                throw new BusinessRuleException("PO_ITEMS_INVALID", "Unsupported PO item data");
            }
            masterDataReferenceClient.requireActiveItem(itemId.trim(), itemName.trim());
        }
    }

    private void validateApprovedIndent(IndentDetails indent, String requestedIndentNumber) {
        if (indent == null) {
            throw new BusinessRuleException("INDENT_SERVICE_INVALID_RESPONSE", "Indent-service returned no indent");
        }
        if (isBlank(indent.indentNumber())) {
            throw new BusinessRuleException("INDENT_SERVICE_INVALID_RESPONSE", "Indent-service did not return indentNumber");
        }
        if (!requestedIndentNumber.equalsIgnoreCase(indent.indentNumber().trim())) {
            throw new BusinessRuleException("INDENT_SERVICE_INVALID_RESPONSE", "Indent-service returned a different indent number");
        }
        if (!"READY_FOR_PO".equalsIgnoreCase(indent.status())) {
            throw new BusinessRuleException(
                    "INDENT_NOT_READY_FOR_PO",
                    "Only an indent in READY_FOR_PO status can be converted to a purchase order: " + requestedIndentNumber);
        }
        if (isBlank(indent.status())) {
            throw new BusinessRuleException("INDENT_STATUS_MISSING", "Indent-service did not return the current indent status");
        }
        if (isBlank(indent.departmentName())) {
            throw new BusinessRuleException("INDENT_DEPARTMENT_MISSING", "Approved indent does not contain departmentName");
        }
        if (indent.items() == null || indent.items().isEmpty()) {
            throw new BusinessRuleException("INDENT_ITEMS_MISSING", "Approved indent does not contain purchase items");
        }
    }

    private void validateApprovedIndentCommand(CreatePurchaseOrderFromApprovedIndentCommand command) {
        if (command == null || isBlank(command.indentNumber())) {
            throw new BusinessRuleException("INDENT_NUMBER_REQUIRED", "Approved indent number is required");
        }
        if (command.purchaseType() == null) {
            throw new BusinessRuleException("PURCHASE_TYPE_REQUIRED", "Purchase type is required");
        }
        if (isBlank(command.vendorName())) {
            throw new BusinessRuleException("VENDOR_NAME_REQUIRED", "Vendor name is required");
        }
    }

    private void validateManualCommand(CreatePurchaseOrderRequest command) {
        if (command == null) {
            throw new BusinessRuleException("INVALID_PO_CREATION_REQUEST", "Purchase order creation request is required");
        }
        if (isBlank(command.vendorName())) {
            throw new BusinessRuleException("VENDOR_NAME_REQUIRED", "Vendor name is required");
        }
        if (isBlank(command.departmentName())) {
            throw new BusinessRuleException("DEPARTMENT_NAME_REQUIRED", "Department name is required");
        }
        if (command.purchaseType() == null) {
            throw new BusinessRuleException("PURCHASE_TYPE_REQUIRED", "Purchase type is required");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new BusinessRuleException("PO_ITEMS_REQUIRED", "At least one PO item is required");
        }
    }

    private PurchaseOrder replay(IdempotencyRecord record) {
        if (record.getResourceId() == null || record.getResourceId().isBlank()) {
            throw new BusinessRuleException("IDEMPOTENCY_RESOURCE_NOT_FOUND", "The previous idempotent operation has no stored purchase order reference");
        }
        return repository.findById(record.getResourceId())
                .orElseThrow(() -> new BusinessRuleException(
                        "IDEMPOTENCY_RESOURCE_NOT_FOUND",
                        "The purchase order created by the previous request no longer exists"));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
