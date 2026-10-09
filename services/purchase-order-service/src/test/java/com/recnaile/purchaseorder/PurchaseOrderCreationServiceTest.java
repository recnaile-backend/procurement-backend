package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderRequest;
import com.recnaile.purchaseorder.client.indent.IndentClient;
import com.recnaile.purchaseorder.client.indent.IndentDetails;
import com.recnaile.purchaseorder.client.masterdata.MasterDataReferenceClient;
import com.recnaile.purchaseorder.client.masterdata.MasterDataVendorClient;
import com.recnaile.purchaseorder.client.masterdata.VendorResponse;
import com.recnaile.purchaseorder.client.masterdata.DepartmentResponse;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.observability.PurchaseOrderMetrics;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import com.recnaile.purchaseorder.service.PurchaseOrderCommercialCalculator;
import com.recnaile.purchaseorder.service.PurchaseOrderCreationService;
import com.recnaile.purchaseorder.service.PurchaseOrderIdempotencyService;
import com.recnaile.purchaseorder.service.PurchaseOrderNumberGenerator;
import com.recnaile.purchaseorder.service.PurchaseOrderOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderCreationServiceTest {

    @Mock PurchaseOrderRepository repository;
    @Mock PurchaseOrderNumberGenerator numberGenerator;
    @Mock PurchaseOrderIdempotencyService idempotencyService;
    @Mock PurchaseOrderOutboxService outboxService;
    @Mock(answer = Answers.CALLS_REAL_METHODS) MasterDataVendorClient masterDataVendorClient;
    @Mock MasterDataReferenceClient masterDataReferenceClient;
    @Mock IndentClient indentClient;
    @Mock PurchaseOrderMetrics metrics;
    @Spy PurchaseOrderCommercialCalculator commercialCalculator;
    @InjectMocks PurchaseOrderCreationService service;

    private VendorResponse vendor() {
        return new VendorResponse("VEN-001", "Vendor One", null, null, null, null, null, null, "ACTIVE", null, null, 1L);
    }

    private IndentDetails readyIndent() {
        return new IndentDetails(
                "IND-001",
                "EMP-001",
                "DEPT-001",
                "Procurement",
                "PRJ-001",
                "Laptop purchase",
                List.of(new IndentDetails.Item(
                        "Laptop", new BigDecimal("2"), new BigDecimal("10000"),
                        new BigDecimal("18"), new BigDecimal("20000"), "Approved item")),
                new BigDecimal("20000"),
                "READY_FOR_PO",
                "TL",
                "PROC-001",
                java.time.Instant.parse("2026-09-21T10:10:34Z"),
                "ok",
                "TL-001",
                java.time.Instant.parse("2026-09-21T10:13:25Z"),
                "OK");
    }

    @Test
    void approvedIndentItemCarriesOnlyItemNameAsThePoBusinessIdentity() {
        var item = new IndentDetails.Item(
                "Bolt", BigDecimal.ONE, new BigDecimal("10000"), BigDecimal.ZERO,
                new BigDecimal("10000"), "M16 Bolts");

        assertThat(item.itemName()).isEqualTo("Bolt");
        assertThat(item.unitPrice()).isEqualByComparingTo("10000.00");
        assertThat(java.util.Arrays.stream(IndentDetails.Item.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList())
                .doesNotContain("itemId", "serialNumber");
    }

    @Test
    void approvedIndentRequestReadsStatusFromIndentService() {
        doReturn(readyIndent()).when(indentClient).getIndent(any());
        doReturn(List.of(vendor())).when(masterDataVendorClient).getAllVendors();
        when(masterDataReferenceClient.requireActiveDepartmentByName("Procurement"))
                .thenReturn(new DepartmentResponse("DEPT-001", "Procurement", "ACTIVE", null, null, 1L));
        when(idempotencyService.requestHash(any(), any(), any())).thenReturn("hash-1");
        when(idempotencyService.reserve(any(), any(), any(), any())).thenReturn(
                new PurchaseOrderIdempotencyService.Reservation(new com.recnaile.purchaseorder.domain.model.IdempotencyRecord(), true));
        when(numberGenerator.nextNumber()).thenReturn("PO-001");
        when(repository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = service.createFromApprovedIndent(
                new CreatePurchaseOrderFromApprovedIndentCommand(
                        "IND-001",
                        com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD,
                        "Vendor One",
                        null),
                "user-1", "create-key-1");

        assertThat(result.getPoId()).isEqualTo("PO-001");
        assertThat(result.getIndentNumber()).isEqualTo("IND-001");
        assertThat(result.getIndentStatus()).isEqualTo("READY_FOR_PO");
        assertThat(result.getPurchaseType()).isEqualTo(com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD);
        assertThat(result.getVendorName()).isEqualTo("Vendor One");
        assertThat(result.getSourceIndentIds()).containsExactly("IND-001");
        assertThat(result.getIndentDetails()).isNotNull();
        assertThat(result.getIndentDetails().getRequesterId()).isEqualTo("EMP-001");
        assertThat(result.getIndentDetails().getVerifierId()).isEqualTo("PROC-001");
        assertThat(result.getIndentDetails().getApproverId()).isEqualTo("TL-001");
        assertThat(result.getSubtotal()).isEqualByComparingTo("20000.00");
        assertThat(result.getGstAmount()).isEqualByComparingTo("3600.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("23600.00");
    }


    @Test
    void rejectsIndentThatWasAlreadyConvertedToPurchaseOrder() {
        doReturn(readyIndent()).when(indentClient).getIndent(any());
        when(idempotencyService.requestHash(any(), any(), any())).thenReturn("hash-duplicate");
        when(idempotencyService.reserve(any(), any(), any(), any())).thenReturn(
                new PurchaseOrderIdempotencyService.Reservation(
                        new com.recnaile.purchaseorder.domain.model.IdempotencyRecord(), true));
        when(repository.findCurrentBySourceIndentIdsIn(any())).thenReturn(List.of(new PurchaseOrder()));

        assertThatThrownBy(() -> service.createFromApprovedIndent(
                new CreatePurchaseOrderFromApprovedIndentCommand(
                        "IND-001",
                        com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD,
                        "Vendor One",
                        null),
                "user-1", "duplicate-key"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been converted");
    }

    @Test
    void rejectsIndentThatIsNotReadyForPo() {
        when(indentClient.getIndent("IND-001"))
                .thenReturn(new IndentDetails(
                        "IND-001", "EMP-001", "DEPT-001", "Procurement", "PRJ-001", "Laptop purchase",
                        List.of(), BigDecimal.ZERO, "APPROVED", "TL", "PROC-001",
                        null, "ok", "TL-001", null, "OK"));
        when(idempotencyService.requestHash(any(), any(), any())).thenReturn("hash-2");
        when(idempotencyService.reserve(any(), any(), any(), any())).thenReturn(
                new PurchaseOrderIdempotencyService.Reservation(new com.recnaile.purchaseorder.domain.model.IdempotencyRecord(), true));

        assertThatThrownBy(() -> service.createFromApprovedIndent(
                new CreatePurchaseOrderFromApprovedIndentCommand(
                        "IND-001",
                        com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD,
                        "Vendor One",
                        null),
                "user-1", "key-2"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("READY_FOR_PO");
    }

    @Test
    void manualCreationValidatesVendorNameAgainstMasterData() {
        doReturn(vendor()).when(masterDataVendorClient).requireActiveVendorByName("Vendor One");
        when(masterDataReferenceClient.requireActiveDepartmentByName("Procurement"))
                .thenReturn(new DepartmentResponse("DEPT-001", "Procurement", "ACTIVE", null, null, 1L));
        when(idempotencyService.requestHash(any(), any(), any())).thenReturn("hash-3");
        when(idempotencyService.reserve(any(), any(), any(), any())).thenReturn(
                new PurchaseOrderIdempotencyService.Reservation(new com.recnaile.purchaseorder.domain.model.IdempotencyRecord(), true));
        when(numberGenerator.nextNumber()).thenReturn("PO-001");
        when(repository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = service.createManual(
                new CreatePurchaseOrderRequest("Vendor One", "PRJ-001", "Procurement", com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD, null,
                        List.of(new CreatePurchaseOrderRequest.Item("Laptop", BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO))),
                "user-1", "key-3");

        assertThat(result.getPoId()).isEqualTo("PO-001");
        assertThat(result.getPurchaseType()).isEqualTo(com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD);
        assertThat(result.getDepartmentId()).isEqualTo("DEPT-001");
        assertThat(result.getVendorName()).isEqualTo("Vendor One");
        assertThat(result.getIndentNumber()).isNull();
    }
    @Test
    void manualCreationWorksWhenIdempotencyKeyIsNotProvided() {
        doReturn(vendor()).when(masterDataVendorClient).requireActiveVendorByName("Vendor One");
        when(masterDataReferenceClient.requireActiveDepartmentByName("Procurement"))
                .thenReturn(new DepartmentResponse("DEPT-001", "Procurement", "ACTIVE", null, null, 1L));
        when(numberGenerator.nextNumber()).thenReturn("PO-002");
        when(repository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder result = service.createManual(
                new CreatePurchaseOrderRequest(
                        "Vendor One",
                        "PRJ-001",
                        "Procurement",
                        com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD,
                        null,
                        List.of(new CreatePurchaseOrderRequest.Item(
                                "Laptop",
                                BigDecimal.ONE,
                                new BigDecimal("100"),
                                BigDecimal.ZERO))),
                "user-1",
                null);

        assertThat(result.getPoId()).isEqualTo("PO-002");
        assertThat(result.getPurchaseType())
                .isEqualTo(com.recnaile.purchaseorder.domain.enums.PurchaseType.STANDARD);
        assertThat(result.getDepartmentId()).isEqualTo("DEPT-001");
        assertThat(result.getIndentNumber()).isNull();
    }

}
