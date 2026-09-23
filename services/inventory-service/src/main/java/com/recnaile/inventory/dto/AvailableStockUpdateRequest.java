package com.recnaile.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(name = "AvailableStockUpdateRequest", description = "Controlled available-stock adjustment. The requested quantity becomes the current available quantity; the difference is recorded as an immutable adjustment movement.")
public record AvailableStockUpdateRequest(
        @Min(value = 0, message = "availableQuantity must be >= 0")
        @Schema(description = "Target available stock quantity.", minimum = "0", example = "50")
        int availableQuantity,

        @Schema(description = "Department name dimension. Optional when the item has a single/default stock balance.", example = "IT")
        String departmentName,

        @Schema(description = "Project dimension. Optional.", example = "PROJ-2001")
        String projectId,

        @Schema(description = "Physical/location dimension. Optional.", example = "RACK-A01")
        String location,

        @Schema(description = "Reason for the controlled adjustment.", example = "Physical stock count correction")
        String reason) {
}
