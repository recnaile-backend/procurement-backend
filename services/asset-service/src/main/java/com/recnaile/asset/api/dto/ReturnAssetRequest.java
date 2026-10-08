package com.recnaile.asset.api.dto;

import com.recnaile.asset.model.AssetCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReturnAssetRequest(
        @NotBlank String outwardNumber,
        @NotBlank String returnId,
        @NotNull AssetCondition condition) {
}
