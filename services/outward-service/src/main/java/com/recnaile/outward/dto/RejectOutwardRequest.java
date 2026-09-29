package com.recnaile.outward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional rejection details.")
public class RejectOutwardRequest {
    @Schema(description = "Reason recorded for rejection", example = "Insufficient business justification", maxLength = 500)
    @Size(max = 500) private String reason;
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
