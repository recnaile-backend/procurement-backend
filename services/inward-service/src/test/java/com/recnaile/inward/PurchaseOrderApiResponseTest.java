package com.recnaile.inward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inward.po.PurchaseOrderApiResponse;
import com.recnaile.inward.po.PurchaseOrderSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderApiResponseTest {

    @Test
    void unwrapsPurchaseOrderFromStandardApiEnvelope() throws Exception {
        String json = """
                {
                  "success": true,
                  "data": {
                    "id": "PO-008",
                    "status": "ISSUED",
                    "vendorName": "3D",
                    "items": []
                  },
                  "error": null
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        var type = mapper.getTypeFactory().constructParametricType(
                PurchaseOrderApiResponse.class, PurchaseOrderSummary.class);

        PurchaseOrderApiResponse<PurchaseOrderSummary> response =
                mapper.readValue(json, type);

        assertTrue(response.success());
        assertNotNull(response.data());
        assertEquals("PO-008", response.data().id());
        assertEquals("ISSUED", response.data().status());
        assertEquals("3D", response.data().vendorName());
        assertEquals(List.of(), response.data().items());
        assertNull(response.error());
    }
}
