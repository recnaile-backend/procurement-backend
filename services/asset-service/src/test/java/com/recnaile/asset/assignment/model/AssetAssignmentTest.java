package com.recnaile.asset.assignment.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AssetAssignmentTest {

    @Test
    void createStartsActive() {
        Instant assignedAt = Instant.parse("2026-10-06T10:00:00Z");
        AssetAssignment assignment = AssetAssignment.create(
                "ASSET%2F26-27%2F001", "ITEM-001", "EMP-001", "IT", "EMP-ADMIN", assignedAt, "Issued");

        assertNotNull(assignment.getAssignmentId());
        assertTrue(assignment.isActive());
        assertEquals("EMP-001", assignment.getAssignedUserId());
        assertNull(assignment.getReturnedAt());
    }

    @Test
    void closePreservesHistory() {
        Instant assignedAt = Instant.parse("2026-10-06T10:00:00Z");
        Instant returnedAt = Instant.parse("2026-10-07T10:00:00Z");
        AssetAssignment assignment = AssetAssignment.create(
                "ASSET%2F26-27%2F001", "ITEM-001", "EMP-001", "IT", "EMP-ADMIN", assignedAt, null);

        assignment.close(returnedAt);

        assertFalse(assignment.isActive());
        assertEquals(returnedAt, assignment.getReturnedAt());
        assertThrows(IllegalStateException.class, () -> assignment.close(returnedAt));
    }

    @Test
    void cannotCloseBeforeAssignment() {
        Instant assignedAt = Instant.parse("2026-10-06T10:00:00Z");
        AssetAssignment assignment = AssetAssignment.create(
                "ASSET%2F26-27%2F001", "ITEM-001", "EMP-001", "IT", "EMP-ADMIN", assignedAt, null);

        assertThrows(IllegalArgumentException.class,
                () -> assignment.close(Instant.parse("2026-10-06T09:59:59Z")));
    }
}
