package com.recnaile.asset.api.dto;

import com.recnaile.asset.model.AssetCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssetRequest(
        @NotBlank String itemId,
        @NotBlank String issueReference,
        @NotBlank String name,
        String brand,
        String model,
        @NotBlank String issuedBy,
        @NotNull AssetCondition condition,
        String remarks) {
}
