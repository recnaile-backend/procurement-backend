package com.recnaile.inventory.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<?> insufficient(InsufficientStockException ex){
        return body(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<?> optimisticConflict(OptimisticLockingFailureException ex){
        return body(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "The inventory balance changed concurrently; retry the operation");
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> stateConflict(IllegalStateException ex){
        return body(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException ex){
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    ResponseEntity<?> duplicate(org.springframework.dao.DuplicateKeyException ex){
        return body(HttpStatus.CONFLICT, "DUPLICATE_OPERATION", "The requested inventory operation has already been applied or conflicts with an existing operation");
    }

    private ResponseEntity<?> body(HttpStatus status, String code, String message) {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("success",false); body.put("data",null);
        body.put("error",Map.of("code",code,"message",message == null ? "" : message,"traceId",Instant.now().toString()));
        return ResponseEntity.status(status).body(body);
    }
}
