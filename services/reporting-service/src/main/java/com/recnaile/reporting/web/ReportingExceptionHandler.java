package com.recnaile.reporting.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice
public class ReportingExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiProblem> badRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid reporting request", safeMessage(ex, "Invalid reporting request"), "INVALID_REQUEST", request);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiProblem> invalidParameter(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid reporting request", "One or more request parameters are missing or invalid.", "INVALID_PARAMETER", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiProblem> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "The HTTP method is not supported for this endpoint.", "METHOD_NOT_ALLOWED", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiProblem> unsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", "The request content type is not supported.", "UNSUPPORTED_MEDIA_TYPE", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiProblem> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Access denied", "The authenticated user does not have the required role or reporting scope.", "FORBIDDEN", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiProblem> notFound(NoResourceFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Resource not found", "No endpoint or resource matches this request path.", "NOT_FOUND", request);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiProblem> dataUnavailable(DataAccessException ex, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Reporting data unavailable", "The reporting data store could not complete the request. Retry later.", "REPORTING_DATA_UNAVAILABLE", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpected(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "The reporting service could not complete the request.", "INTERNAL_ERROR", request);
    }

    private ResponseEntity<ApiProblem> response(HttpStatus status, String title, String detail, String code, HttpServletRequest request) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) correlationId = request.getHeader("X-Correlation-ID");
        ApiProblem problem = new ApiProblem(URI.create("about:blank"), title, status.value(), detail,
                URI.create(request.getRequestURI()), code, correlationId);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    private String safeMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
