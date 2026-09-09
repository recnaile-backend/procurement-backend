package com.recnaile.auth.exception;

import com.recnaile.auth.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        String message = ex.getMessage();

        /*
         * Duplicate employee ID.
         *
         * Employee ID is now the unique business identifier
         * used for registration and login.
         */
        if ("Employee ID already registered".equals(message)) {
            status = HttpStatus.CONFLICT;
        }

        return buildResponse(
            status,
            message,
            request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {

        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error ->
                error.getField() +
                ": " +
                error.getDefaultMessage()
            )
            .collect(Collectors.joining(", "));

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            message,
            request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
        HttpStatus status,
        String message,
        String path
    ) {

        ErrorResponse response = new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path
        );

        return ResponseEntity
            .status(status)
            .body(response);
    }
}