package com.recnaile.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Transport payload used by an authorized verifier to complete return verification.
 * Authentication/RBAC enforcement is introduced in the security phase.
 */
public record VerifyReturnRequest(
        @NotBlank(message = "verifiedBy is required")
        @Size(max = 100, message = "verifiedBy must not exceed 100 characters")
        String verifiedBy) {
}
