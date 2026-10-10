package com.recnaile.audit.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiResponse.failure("VALIDATION_ERROR", exception.getMessage(), traceId(request)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> invalidParameter(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiResponse.failure("VALIDATION_ERROR", "Invalid value for parameter: " + exception.getName(), traceId(request)));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> internalError(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure("INTERNAL_ERROR", "An unexpected error occurred", traceId(request)));
    }
    private static String traceId(HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) return traceId;
        return null;
    }
}
