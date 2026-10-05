package com.recnaile.returns.api;

public record ApiError(String code, String message, String traceId) {
}
