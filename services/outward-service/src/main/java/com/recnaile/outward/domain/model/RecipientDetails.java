package com.recnaile.outward.domain.model;

import jakarta.validation.constraints.NotBlank;

public class RecipientDetails {

    @NotBlank
    private String employeeId;

    @NotBlank
    private String employeeName;

    public RecipientDetails() {
    }

    public RecipientDetails(String employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
}
