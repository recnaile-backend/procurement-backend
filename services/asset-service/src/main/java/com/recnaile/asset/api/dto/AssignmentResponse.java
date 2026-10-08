package com.recnaile.asset.api.dto;

import java.time.Instant;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.model.AssetCondition;

public record AssignmentResponse(
        String assignmentId, String assetId, String employeeId, String departmentName,
        String issuedBy, String outwardNumber, Instant assignedAt, Instant returnedAt,
        String returnId, AssetCondition returnCondition, String remarks) {
    public static AssignmentResponse from(AssetAssignment a) {
        return new AssignmentResponse(a.getAssignmentId(), a.getAssetId(), a.getEmployeeId(), a.getDepartmentName(),
                a.getIssuedBy(), a.getOutwardNumber(), a.getAssignedAt(), a.getReturnedAt(), a.getReturnId(),
                a.getReturnCondition(), a.getRemarks());
    }
}
