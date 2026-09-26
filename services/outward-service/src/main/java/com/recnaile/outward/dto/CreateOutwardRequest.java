package com.recnaile.outward.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateOutwardRequest {

    @Valid
    @NotEmpty
    @Size(max = 100)
    private List<CreateOutwardItem> items;

    @Size(max = 100)
    private String projectId;

    @NotBlank
    @Size(max = 100)
    private String departmentName;

    @NotBlank
    @Size(max = 500)
    private String purpose;

    @Valid
    @NotNull
    private RecipientRequest recipient;

    public List<CreateOutwardItem> getItems() { return items; }
    public void setItems(List<CreateOutwardItem> items) { this.items = items; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public RecipientRequest getRecipient() { return recipient; }
    public void setRecipient(RecipientRequest recipient) { this.recipient = recipient; }
}
