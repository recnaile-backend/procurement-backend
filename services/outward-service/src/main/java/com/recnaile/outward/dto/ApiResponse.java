package com.recnaile.outward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard success/error response envelope.")
public record ApiResponse<T>(
        @Schema(description = "Whether the operation succeeded.", example = "true")
        boolean success,
        T data,
        ApiError error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }
}
