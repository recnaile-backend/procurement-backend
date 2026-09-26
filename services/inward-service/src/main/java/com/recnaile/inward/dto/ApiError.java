package com.recnaile.inward.dto;

public record ApiError(
        String code,
        String message,
        String traceId
) {
}
