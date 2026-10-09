package com.recnaile.returns.dto;

import jakarta.validation.constraints.Size;

/**
 * Transport payload used by an authorized verifier to complete return verification.
 * The authenticated JWT principal is authoritative; verifiedBy must match that principal when supplied.
 */
public record VerifyReturnRequest(
        @Size(max = 100, message = "verifiedBy must not exceed 100 characters")
        String verifiedBy) {
}
