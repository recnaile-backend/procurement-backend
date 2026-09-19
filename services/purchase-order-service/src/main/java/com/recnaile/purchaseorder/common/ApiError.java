package com.recnaile.purchaseorder.common;

public record ApiError(
        String code,
        String message,
        String traceId) {
}
