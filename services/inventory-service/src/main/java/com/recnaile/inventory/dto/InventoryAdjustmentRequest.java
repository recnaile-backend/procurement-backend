package com.recnaile.inventory.dto;

import com.recnaile.inventory.domain.StockType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "InventoryAdjustmentRequest", description = "Controlled administrative stock correction. Event-driven Inward, Outward and Return transactions remain the normal stock path.")
public record InventoryAdjustmentRequest(
        @Schema(description = "Department dimension. Optional.", example = "IT") String departmentName,
        @Schema(description = "Project dimension. Optional.", example = "PROJ-2001") String projectId,
        @Schema(description = "Physical/location dimension. Optional.", example = "RACK-A01") String location,
        @NotNull @Schema(description = "Stock bucket to adjust.", example = "AVAILABLE") StockType stockType,
        @Schema(description = "Signed quantity delta. Positive adds stock; negative removes stock.", example = "5") int delta,
        @Schema(description = "Business reason for the correction.", example = "Physical stock count correction") String reason) {
}
