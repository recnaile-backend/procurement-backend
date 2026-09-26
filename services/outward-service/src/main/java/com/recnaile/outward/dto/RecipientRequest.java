package com.recnaile.outward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecipientRequest {

    @NotBlank
    @Size(max = 100)
    private String employeeId;

    @NotBlank
    @Size(max = 200)
    private String employeeName;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
}
