package com.recnaile.outward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Request to create a DRAFT outward request.")
public class CreateOutwardRequest {
    @Schema(description = "Items to issue", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid @NotEmpty @Size(max = 100) private List<CreateOutwardItem> items;
    @Schema(description = "Optional active Master Data project", example = "PROJ-001")
    @Size(max = 100) private String projectId;
    @Schema(description = "Active department name", example = "Engineering", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 100) private String departmentName;
    @Schema(description = "Business purpose for the outward issue", example = "Laptop issue for project work", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 500) private String purpose;
    @Schema(description = "Recipient details", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid @NotNull private RecipientRequest recipient;
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
