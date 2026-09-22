package com.recnaile.indent.dto.response;

public record ApiError(
        String code,
        String message,
        String traceId
) {}
