package com.recnaile.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AssetAssignmentTest {

    @Test
    void startShouldCreateActiveHistoricalAssignment() {
        Instant assignedAt = Instant.parse("2026-10-05T08:00:00Z");

        AssetAssignment assignment = AssetAssignment.start(
                "asset-001", "EMP-001", "Engineering", "PROC-001",
                "OUT/26-27/001", assignedAt, "Laptop issued");

        assertNotNull(assignment.getAssignmentId());
        assertEquals("asset-001", assignment.getAssetId());
        assertEquals("EMP-001", assignment.getEmployeeId());
        assertEquals("Engineering", assignment.getDepartmentName());
        assertEquals("OUT/26-27/001", assignment.getOutwardId());
        assertEquals(assignedAt, assignment.getAssignedAt());
        assertTrue(assignment.isActive());
    }

    @Test
    void closeShouldPreserveReturnDetailsAndMakeAssignmentInactive() {
        Instant assignedAt = Instant.parse("2026-10-05T08:00:00Z");
        Instant returnedAt = Instant.parse("2026-10-10T08:00:00Z");
        AssetAssignment assignment = AssetAssignment.start(
                "asset-001", "EMP-001", "Engineering", "PROC-001",
                "OUT/26-27/001", assignedAt, null);

        assignment.close(returnedAt, "RET/26-27/001", AssetCondition.GOOD);

        assertFalse(assignment.isActive());
        assertEquals(returnedAt, assignment.getReturnedAt());
        assertEquals("RET/26-27/001", assignment.getReturnId());
        assertEquals(AssetCondition.GOOD, assignment.getReturnCondition());
    }

    @Test
    void closeShouldRejectSecondClose() {
        AssetAssignment assignment = AssetAssignment.start(
                "asset-001", "EMP-001", "Engineering", "PROC-001",
                "OUT/26-27/001", Instant.parse("2026-10-05T08:00:00Z"), null);
        assignment.close(
                Instant.parse("2026-10-10T08:00:00Z"),
                "RET/26-27/001",
                AssetCondition.GOOD);

        assertThrows(IllegalStateException.class, () -> assignment.close(
                Instant.parse("2026-10-11T08:00:00Z"),
                "RET/26-27/002",
                AssetCondition.GOOD));
    }

    @Test
    void closeShouldRejectReturnBeforeAssignment() {
        AssetAssignment assignment = AssetAssignment.start(
                "asset-001", "EMP-001", "Engineering", "PROC-001",
                "OUT/26-27/001", Instant.parse("2026-10-05T08:00:00Z"), null);

        assertThrows(IllegalArgumentException.class, () -> assignment.close(
                Instant.parse("2026-10-04T08:00:00Z"),
                "RET/26-27/001",
                AssetCondition.GOOD));
    }
}
