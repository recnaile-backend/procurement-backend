package com.recnaile.outward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Recipient captured on the outward request.")
public class RecipientRequest {
    @Schema(description = "Recipient employee identifier", example = "EMP-1001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 100) private String employeeId;
    @Schema(description = "Recipient employee display name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 200) private String employeeName;
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
}
