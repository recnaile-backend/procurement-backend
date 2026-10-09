package com.recnaile.returns.controller;

import com.recnaile.returns.api.ApiError;
import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.exception.DownstreamServiceException;
import com.recnaile.returns.exception.ReturnNotFoundException;
import com.recnaile.returns.exception.IdempotencyConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MissingRequestHeaderException;

@RestControllerAdvice
public class ReturnExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(error("VALIDATION_ERROR", message)));
    }


    @ExceptionHandler(ReturnNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ReturnNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(error("RETURN_NOT_FOUND", exception.getMessage())));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyConflict(IdempotencyConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(error("IDEMPOTENCY_CONFLICT", exception.getMessage())));
    }

    @ExceptionHandler({
            DuplicateKeyException.class,
            OptimisticLockingFailureException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleConcurrencyConflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(error(
                        "CONCURRENT_UPDATE",
                        "The return was changed concurrently or conflicts with an existing record")));
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDownstream(DownstreamServiceException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(error("DOWNSTREAM_SERVICE_UNAVAILABLE", exception.getMessage())));
    }

    @ExceptionHandler({IllegalArgumentException.class, ReturnRequestException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(error("INVALID_RETURN_REQUEST", exception.getMessage())));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(error("MALFORMED_REQUEST", "Request body is malformed or contains an invalid value")));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(error("MISSING_HEADER", exception.getHeaderName() + " header is required")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(error("INTERNAL_SERVER_ERROR", "An unexpected error occurred")));
    }

    private ApiError error(String code, String message) {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("correlationId");
        }
        return new ApiError(code, message, traceId);
    }

}
