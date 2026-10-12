package com.recnaile.audit.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(Exception exception, HttpServletRequest request) {
        String message = "Request validation failed";
        if (exception instanceof IllegalArgumentException && exception.getMessage() != null) {
            message = exception.getMessage();
        } else if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            message = "Invalid value for parameter: " + mismatch.getName();
        } else if (exception instanceof MethodArgumentNotValidException invalid && invalid.getBindingResult().getFieldError() != null) {
            var fieldError = invalid.getBindingResult().getFieldError();
            message = "Invalid field '" + fieldError.getField() + "': " + fieldError.getDefaultMessage();
        } else if (exception instanceof ConstraintViolationException violation && !violation.getConstraintViolations().isEmpty()) {
            message = violation.getConstraintViolations().iterator().next().getMessage();
        } else if (exception instanceof HttpMessageNotReadableException) {
            message = "Request body is missing or contains invalid JSON";
        }
        return ResponseEntity.badRequest().body(ApiResponse.failure("VALIDATION_ERROR", message, traceId(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.failure("FORBIDDEN", "You are not authorized to access this audit data", traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> internalError(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.failure("INTERNAL_ERROR", "An unexpected error occurred", traceId(request)));
    }

    private static String traceId(HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        return traceId != null && !traceId.isBlank() ? traceId : null;
    }
}
