package com.recnaile.purchaseorder.exception;

import com.recnaile.purchaseorder.common.ApiError;
import com.recnaile.purchaseorder.common.ApiResponse;
import com.recnaile.purchaseorder.observability.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MasterDataIntegrationException.class)
    ResponseEntity<ApiResponse<Void>> handleMasterDataIntegration(
            MasterDataIntegrationException ex, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE,
                ex.getCode(),
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(ConcurrentPurchaseOrderModificationException.class)
    ResponseEntity<ApiResponse<Void>> handleConcurrency(
            ConcurrentPurchaseOrderModificationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                "PO_CONCURRENT_MODIFICATION",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                "PO_CONCURRENT_MODIFICATION",
                "Purchase Order was modified by another request. Refresh and retry the operation.",
                request);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiResponse<Void>> handleDuplicateKey(
            DuplicateKeyException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                "The requested purchase order operation conflicts with an existing resource.",
                request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        if ("Idempotency-Key".equalsIgnoreCase(ex.getHeaderName())) {
            return response(HttpStatus.BAD_REQUEST,
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key header is required for this operation",
                    request);
        }
        return response(HttpStatus.BAD_REQUEST,
                "REQUEST_HEADER_REQUIRED",
                "Required request header is missing: " + ex.getHeaderName(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request);
    }

    private ResponseEntity<ApiResponse<Void>> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {

        String traceId = request.getHeader(CorrelationIdFilter.HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = "unknown";
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.failure(new ApiError(code, message, traceId)));
    }
}
