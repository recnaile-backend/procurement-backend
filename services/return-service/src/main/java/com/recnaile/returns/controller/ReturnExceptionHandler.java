package com.recnaile.returns.controller;

import com.recnaile.returns.api.ApiError;
import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.exception.ReturnNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ReturnExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(new ApiError("VALIDATION_ERROR", message, null)));
    }


    @ExceptionHandler(ReturnNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ReturnNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(new ApiError("RETURN_NOT_FOUND", exception.getMessage(), null)));
    }

    @ExceptionHandler({IllegalArgumentException.class, ReturnRequestException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(new ApiError("INVALID_RETURN_REQUEST", exception.getMessage(), null)));
    }
}
