package com.recnaile.inward;

import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.model.GRNStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GRNDomainModelTest {

    @Test
    void grnContainsReceiptHeaderAndLineLevelQuantities() {
        GRNLine line = new GRNLine(
                "Bolt",
                new BigDecimal("10"),
                new BigDecimal("8"),
                new BigDecimal("2"),
                "PCS",
                "2 units damaged"
        );

        GRNDocument grn = new GRNDocument(
                null,
                "GRN%2F26-27%2F001",
                "PO-001",
                "3D",
                "DN-001",
                Instant.parse("2026-09-23T08:00:00Z"),
                Instant.parse("2026-09-23T09:00:00Z"),
                "user-001",
                GRNStatus.DRAFT,
                List.of(line)
        );

        assertEquals("GRN%2F26-27%2F001", grn.getGrnId());
        assertEquals("PO-001", grn.getPoId());
        assertEquals("3D", grn.getVendorName());
        assertEquals(GRNStatus.DRAFT, grn.getGrnStatus());
        assertEquals(1, grn.getLines().size());
        assertEquals(new BigDecimal("8"), grn.getLines().get(0).acceptedQuantity());
        assertEquals(new BigDecimal("2"), grn.getLines().get(0).rejectedQuantity());
        assertEquals("2 units damaged", grn.getLines().get(0).rejectionReason());
    }
}
