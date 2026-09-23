package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.dto.PurchaseOrderResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderResponseTest {

    @Test
    void mapsLegacyPurchaseOrderWithNullCollectionsWithoutThrowing() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId("PO-001");
        po.setSourceIndentIds(null);
        po.setLines(null);

        PurchaseOrderResponse response = PurchaseOrderResponse.from(po);

        assertThat(response.poId()).isEqualTo("PO-001");
        assertThat(response.indentNumber()).isNull();
        assertThat(response.items()).isEmpty();
    }
}
