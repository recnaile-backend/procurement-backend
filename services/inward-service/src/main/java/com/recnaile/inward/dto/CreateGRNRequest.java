package com.recnaile.inward.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateGRNRequest(
        @NotBlank String poId,
        @NotBlank String deliveryNoteNumber,
        @NotNull Instant deliveryDate,
        @NotBlank String receivedBy,
        @NotEmpty List<@Valid CreateGRNLineRequest> items
) {
}
