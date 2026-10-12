package com.recnaile.reporting.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ReportingReadModelTest {
    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Test void factRequiresStableEventIdentityAndTimestamps() {
        assertThrows(IllegalArgumentException.class, () -> new ReportingFact(
            " ", "INDENT_SUBMITTED", NOW, "indent-service", "INDENT", "IND-1", null,
            null, null, null, null, "PENDING_VERIFICATION", null, null,
            null, null, null, null, null, null, null, null, null, null, NOW));
    }

    @Test void projectionKeyIsStableForEntityTypeAndId() {
        assertEquals("INDENT:IND-42", ReportingProjection.keyFor("INDENT", "IND-42"));
        assertThrows(IllegalArgumentException.class, () -> ReportingProjection.keyFor("INDENT", " "));
    }

    @Test void projectionRejectsIdentityMismatch() {
        assertThrows(IllegalArgumentException.class, () -> new ReportingProjection(
            "INDENT:IND-1", "INDENT", "IND-2", "APPROVED", null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null,
            NOW, NOW, NOW, "event-1", "INDENT_APPROVED"));
    }

    @Test void projectionAcceptsConsistentTypedReadModel() {
        ReportingProjection projection = new ReportingProjection(
            ReportingProjection.keyFor("INDENT", "IND-1"), "INDENT", "IND-1", "APPROVED",
            "D-1", "P-1", null, null, null, null, null, 1200.0, null, null, null,
            null, null, "U-1", "indent-service", NOW, NOW, NOW, "event-1", "INDENT_APPROVED");
        assertEquals("INDENT:IND-1", projection.id());
        assertEquals("IND-1", projection.entityId());
    }
}
