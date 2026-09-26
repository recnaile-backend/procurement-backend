package com.recnaile.outward.exception;

import java.util.UUID;

import com.recnaile.outward.dto.response.ApiError;
import com.recnaile.outward.dto.response.ApiResponse;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.MDC;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OutwardException.class)
    ResponseEntity<ApiResponse<Void>> handleOutward(OutwardException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "OUTWARD_NOT_FOUND", "ITEM_NOT_FOUND", "DEPARTMENT_NOT_FOUND",
                    "PROJECT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ACCESS_DENIED", "APPROVAL_ROLE_NOT_ALLOWED", "RECIPIENT_NOT_ALLOWED" -> HttpStatus.FORBIDDEN;
            case "INSUFFICIENT_AVAILABLE_STOCK", "IDEMPOTENCY_CONFLICT",
                    "IDEMPOTENCY_KEY_CONFLICT", "IDEMPOTENCY_IN_PROGRESS",
                    "OUTWARD_CONCURRENCY_CONFLICT", "INVALID_OUTWARD_STATE_TRANSITION" -> HttpStatus.CONFLICT;
            case "MASTER_DATA_SERVICE_UNAVAILABLE", "INVENTORY_SERVICE_UNAVAILABLE",
                    "INVENTORY_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.failure(error(ex.getCode(), ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Request validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(error("VALIDATION_ERROR", message)));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<Void>> handleOptimistic(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(error("OUTWARD_CONCURRENCY_CONFLICT",
                        "The outward request was modified by another request. Please refresh and retry.")));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(error("INTERNAL_ERROR", "Unexpected server error")));
    }

    private ApiError error(String code, String message) {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("correlationId");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return new ApiError(code, message, traceId);
    }
}
