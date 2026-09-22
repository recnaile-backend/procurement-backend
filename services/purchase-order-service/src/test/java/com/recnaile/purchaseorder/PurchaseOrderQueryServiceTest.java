package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import com.recnaile.purchaseorder.service.PurchaseOrderQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PurchaseOrderQueryServiceTest {

    @Test
    void noFiltersUsesCurrentPurchaseOrdersOnly() {
        PurchaseOrderRepository repository = mock(PurchaseOrderRepository.class);
        PurchaseOrderQueryService service = new PurchaseOrderQueryService(repository);
        List<PurchaseOrder> expected = List.of(new PurchaseOrder());
        when(repository.findAllCurrent()).thenReturn(expected);

        assertThat(service.find(null, null, null)).isSameAs(expected);
        verify(repository).findAllCurrent();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void departmentNameFilterIsOptionalAndUsesDepartmentRepositoryQuery() {
        PurchaseOrderRepository repository = mock(PurchaseOrderRepository.class);
        PurchaseOrderQueryService service = new PurchaseOrderQueryService(repository);
        List<PurchaseOrder> expected = List.of(new PurchaseOrder());
        when(repository.findCurrentByDepartmentName("Software")).thenReturn(expected);

        assertThat(service.find(null, null, "Software")).isSameAs(expected);
        verify(repository).findCurrentByDepartmentName("Software");
    }

    @Test
    void allThreeFiltersUseAndSemantics() {
        PurchaseOrderRepository repository = mock(PurchaseOrderRepository.class);
        PurchaseOrderQueryService service = new PurchaseOrderQueryService(repository);
        List<PurchaseOrder> expected = List.of(new PurchaseOrder());
        when(repository.findCurrentByVendorNameAndProjectIdAndDepartmentName(
                "Vendor A", "P-1", "Software")).thenReturn(expected);

        assertThat(service.find("Vendor A", "P-1", "Software")).isSameAs(expected);
        verify(repository).findCurrentByVendorNameAndProjectIdAndDepartmentName(
                "Vendor A", "P-1", "Software");
    }

    @Test
    void blankFiltersAreTreatedAsMissing() {
        PurchaseOrderRepository repository = mock(PurchaseOrderRepository.class);
        PurchaseOrderQueryService service = new PurchaseOrderQueryService(repository);
        List<PurchaseOrder> expected = List.of(new PurchaseOrder());
        when(repository.findAllCurrent()).thenReturn(expected);

        assertThat(service.find(" ", "", null)).isSameAs(expected);
        verify(repository).findAllCurrent();
    }
}
