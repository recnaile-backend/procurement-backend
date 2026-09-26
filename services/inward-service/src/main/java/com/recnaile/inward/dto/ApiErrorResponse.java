package com.recnaile.inward.dto;

public record ApiErrorResponse(
        boolean success,
        Object data,
        ApiError error
) {
    public static ApiErrorResponse of(String code, String message, String traceId) {
        return new ApiErrorResponse(false, null, new ApiError(code, message, traceId));
    }
}
