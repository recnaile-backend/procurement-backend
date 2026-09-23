package com.recnaile.indent.exception;

import com.recnaile.indent.client.masterdata.MasterDataException;
import com.recnaile.indent.exception.InvalidIndentTransitionException;
import com.recnaile.indent.dto.response.ApiError;
import com.recnaile.indent.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request);
    }

    @ExceptionHandler(IndentValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIndentValidation(
            IndentValidationException ex,
            HttpServletRequest request) {

        HttpStatus status;
        if (ex.getCode().endsWith("_NOT_FOUND")) {
            status = HttpStatus.NOT_FOUND;
        } else if ("MASTER_DATA_UNAVAILABLE".equals(ex.getCode())) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        }

        return response(
                status,
                ex.getCode(),
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(InvalidIndentTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransition(
            InvalidIndentTransitionException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.CONFLICT,
                "INVALID_STATE_TRANSITION",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyConflict(
            IdempotencyKeyConflictException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_KEY_REUSED",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(ConcurrentIndentModificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentModification(
            ConcurrentIndentModificationException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.CONFLICT,
                "CONCURRENT_MODIFICATION",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(ApprovalConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApprovalConfiguration(
            ApprovalConfigurationException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "APPROVAL_CONFIGURATION_ERROR",
                "Approval routing configuration is invalid",
                request);
    }

    @ExceptionHandler(MasterDataException.class)
    public ResponseEntity<ApiResponse<Void>> handleMasterData(
            MasterDataException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MASTER_DATA_UNAVAILABLE",
                "Master Data validation is temporarily unavailable",
                request);
    }

    private ResponseEntity<ApiResponse<Void>> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {

        String traceId =
                request.getHeader("X-Correlation-Id");

        ApiError error =
                new ApiError(
                        code,
                        message,
                        traceId);

        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(error));
    }
}