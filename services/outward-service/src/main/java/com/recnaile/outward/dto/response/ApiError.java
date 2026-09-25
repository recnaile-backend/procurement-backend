package com.recnaile.outward.dto.response;

public record ApiError(
        String code,
        String message,
        String traceId
) {}
