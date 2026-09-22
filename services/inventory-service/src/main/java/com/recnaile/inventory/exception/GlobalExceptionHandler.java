package com.recnaile.inventory.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<?> insufficient(InsufficientStockException ex){
        Map<String,Object> body=new java.util.LinkedHashMap<>();
        body.put("success",false); body.put("data",null);
        body.put("error",Map.of("code","BUSINESS_ERROR","message",ex.getMessage(),"traceId",Instant.now().toString()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException ex){
        Map<String,Object> body=new java.util.LinkedHashMap<>();
        body.put("success",false); body.put("data",null);
        body.put("error",Map.of("code","VALIDATION_ERROR","message",ex.getMessage(),"traceId",Instant.now().toString()));
        return ResponseEntity.badRequest().body(body);
    }
}
