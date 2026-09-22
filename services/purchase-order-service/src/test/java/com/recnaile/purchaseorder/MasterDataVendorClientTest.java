package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.client.masterdata.MasterDataVendorClient;
import com.recnaile.purchaseorder.client.masterdata.VendorResponse;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.exception.MasterDataIntegrationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MasterDataVendorClientTest {

    @Test
    void requireActiveVendor_shouldRejectInactiveVendor() {
        MasterDataVendorClient client = mock(MasterDataVendorClient.class, org.mockito.Answers.CALLS_REAL_METHODS);
        when(client.getVendor("VEN-001")).thenReturn(new VendorResponse(
                "VEN-001", "Vendor", null, null, null, null, null, null,
                "INACTIVE", null, null, 1L));

        assertThatThrownBy(() -> client.requireActiveVendor("VEN-001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void requireActiveVendor_shouldRejectEmptyVendorResponse() {
        MasterDataVendorClient client =
                mock(MasterDataVendorClient.class, org.mockito.Answers.CALLS_REAL_METHODS);
        when(client.getVendor("VEN-001")).thenReturn(null);

        assertThatThrownBy(() -> client.requireActiveVendor("VEN-001"))
                .isInstanceOf(MasterDataIntegrationException.class)
                .hasMessageContaining("empty vendor response");
    }

    @Test
    void requireActiveVendorByName_shouldResolveActiveVendor() {
        MasterDataVendorClient client =
                mock(MasterDataVendorClient.class, org.mockito.Answers.CALLS_REAL_METHODS);
        VendorResponse vendor = new VendorResponse(
                "VEN-001", "ABC Supplies", null, null, null, null, null, null,
                "ACTIVE", null, null, 1L);
        when(client.getAllVendors()).thenReturn(List.of(vendor));

        assertThat(client.requireActiveVendorByName("abc supplies")).isEqualTo(vendor);
    }
}
