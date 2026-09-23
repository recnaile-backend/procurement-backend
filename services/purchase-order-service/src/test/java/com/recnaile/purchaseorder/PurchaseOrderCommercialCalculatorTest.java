package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderLine;
import com.recnaile.purchaseorder.service.PurchaseOrderCommercialCalculator;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseOrderCommercialCalculatorTest {

    private final PurchaseOrderCommercialCalculator calculator = new PurchaseOrderCommercialCalculator();

    @Test
    void shouldCalculateLineAndPoTotalsOnServer() {
        PurchaseOrderLine line = line("ITEM-1", "3", "999.99", "18");
        PurchaseOrder po = new PurchaseOrder();
        po.setLines(List.of(line));

        calculator.calculate(po);

        assertThat(line.getLineSubtotal()).isEqualByComparingTo("2999.97");
        assertThat(line.getGstAmount()).isEqualByComparingTo("539.99");
        assertThat(line.getLineTotal()).isEqualByComparingTo("3539.96");
        assertThat(po.getSubtotal()).isEqualByComparingTo("2999.97");
        assertThat(po.getGstAmount()).isEqualByComparingTo("539.99");
        assertThat(po.getTotalAmount()).isEqualByComparingTo("3539.96");
    }

    @Test
    void shouldRoundMoneyToTwoDecimals() {
        PurchaseOrderLine line = line("ITEM-1", "1", "0.01", "18");
        PurchaseOrder po = new PurchaseOrder();
        po.setLines(List.of(line));

        calculator.calculate(po);

        assertThat(line.getLineSubtotal()).isEqualByComparingTo("0.01");
        assertThat(line.getGstAmount()).isEqualByComparingTo("0.00");
        assertThat(line.getLineTotal()).isEqualByComparingTo("0.01");
    }

    @Test
    void shouldRejectNegativeValues() {
        PurchaseOrderLine line = line("ITEM-1", "1", "-10", "18");
        PurchaseOrder po = new PurchaseOrder();
        po.setLines(List.of(line));

        assertThatThrownBy(() -> calculator.calculate(po))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("unitPrice");
    }

    @Test
    void shouldRejectReceivedQuantityAboveOrderedQuantity() {
        PurchaseOrderLine line = line("ITEM-1", "2", "100", "18");
        line.setReceivedQuantity(new BigDecimal("3"));
        PurchaseOrder po = new PurchaseOrder();
        po.setLines(List.of(line));

        assertThatThrownBy(() -> calculator.calculate(po))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Received quantity cannot exceed ordered quantity");
    }

    private PurchaseOrderLine line(String itemId, String quantity, String price, String gst) {
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setItemId(itemId);
        line.setOrderedQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal(price));
        line.setGstRate(new BigDecimal(gst));
        return line;
    }
}
