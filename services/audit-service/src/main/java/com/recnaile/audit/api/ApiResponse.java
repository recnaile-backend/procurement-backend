package com.recnaile.audit.api;

/** Standard platform success/error envelope. */
public record ApiResponse<T>(boolean success, T data, ApiError error) {
    public static <T> ApiResponse<T> success(T data) { return new ApiResponse<>(true, data, null); }
    public static <T> ApiResponse<T> failure(String code, String message, String traceId) {
        return new ApiResponse<>(false, null, new ApiError(code, message, traceId));
    }
    public record ApiError(String code, String message, String traceId) { }
}
