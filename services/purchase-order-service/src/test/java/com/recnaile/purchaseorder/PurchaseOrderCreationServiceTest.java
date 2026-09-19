package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.application.command.CreatePurchaseOrderFromApprovedIndentCommand;
import com.recnaile.purchaseorder.service.PurchaseOrderCreationService;
import com.recnaile.purchaseorder.service.PurchaseOrderNumberGenerator;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.service.PurchaseOrderCommercialCalculator;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import com.recnaile.purchaseorder.service.PurchaseOrderIdempotencyService;
import com.recnaile.purchaseorder.service.PurchaseOrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderCreationServiceTest {

    @Mock
    PurchaseOrderRepository repository;

    @Mock
    PurchaseOrderNumberGenerator numberGenerator;

    @Mock
    PurchaseOrderIdempotencyService idempotencyService;

    @Mock
    PurchaseOrderEventPublisher eventPublisher;

    @Spy
    PurchaseOrderCommercialCalculator commercialCalculator;

    @InjectMocks
    PurchaseOrderCreationService service;

    @Test
    void shouldCreatePoFromReadyForPoIndent() {
        when(idempotencyService.requestHash(any(), any(), any())).thenReturn("hash-1");
        when(idempotencyService.reserve(any(), any(), any(), any()))
                .thenReturn(new PurchaseOrderIdempotencyService.Reservation(
                        new com.recnaile.purchaseorder.domain.model.IdempotencyRecord(), true));
        when(repository.findBySourceIndentIdsIn(any())).thenReturn(List.of());
        when(numberGenerator.nextNumber(any())).thenReturn("PO-2026-0001");
        when(repository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var command = new CreatePurchaseOrderFromApprovedIndentCommand(
                "IND-001", "VEN-001", "READY_FOR_PO", "PRJ-001", "DEP-001", null, null,
                List.of(new CreatePurchaseOrderFromApprovedIndentCommand.ApprovedIndentLine(
                        "ITEM-001", "Laptop", new BigDecimal("2"),
                        new BigDecimal("10000"), new BigDecimal("18"))));

        PurchaseOrder result = service.createFromApprovedIndent(command, "user-1", "create-key-1");

        assertThat(result.getPoNumber()).isEqualTo("PO-2026-0001");
        assertThat(result.getSourceIndentIds()).containsExactly("IND-001");
        assertThat(result.getLines()).hasSize(1);
        assertThat(result.getLines().getFirst().getRemainingQuantity()).isEqualByComparingTo("2");
        assertThat(result.getSubtotal()).isEqualByComparingTo("20000.00");
        assertThat(result.getGstAmount()).isEqualByComparingTo("3600.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("23600.00");
    }

    @Test
    void shouldRejectIndentThatIsNotReadyForPo() {
        var command = new CreatePurchaseOrderFromApprovedIndentCommand(
                "IND-001", "VEN-001", "APPROVED", "PRJ-001", "DEP-001", null, null,
                List.of(new CreatePurchaseOrderFromApprovedIndentCommand.ApprovedIndentLine(
                        "ITEM-001", "Laptop", new BigDecimal("1"),
                        new BigDecimal("100"), BigDecimal.ZERO)));

        assertThatThrownBy(() -> service.createFromApprovedIndent(command, "user-1", "create-key-test"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("READY_FOR_PO");
    }

    @Test
    void shouldRejectDuplicateIndentConversion() {
        when(repository.findBySourceIndentIdsIn(any()))
                .thenReturn(List.of(new PurchaseOrder()));

        var command = new CreatePurchaseOrderFromApprovedIndentCommand(
                "IND-001", "VEN-001", "READY_FOR_PO", "PRJ-001", "DEP-001", null, null,
                List.of(new CreatePurchaseOrderFromApprovedIndentCommand.ApprovedIndentLine(
                        "ITEM-001", "Laptop", BigDecimal.ONE,
                        new BigDecimal("100"), BigDecimal.ZERO)));

        when(idempotencyService.requestHash(any(), any(), any())).thenReturn("hash-2");
        when(idempotencyService.reserve(any(), any(), any(), any()))
                .thenReturn(new PurchaseOrderIdempotencyService.Reservation(
                        new com.recnaile.purchaseorder.domain.model.IdempotencyRecord(), true));

        PurchaseOrder existing = new PurchaseOrder();
        existing.setSourceIndentIds(List.of("IND-001"));
        when(repository.findBySourceIndentIdsIn(any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createFromApprovedIndent(command, "user-1", "create-key-test"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already linked");
    }
}
