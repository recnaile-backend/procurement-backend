package com.recnaile.outward.exception;

import com.recnaile.outward.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OutwardExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "OUTWARD_VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex,
                                                      HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "OUTWARD_VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(MasterDataValidationException.class)
    public ResponseEntity<ApiError> handleMasterDataValidation(MasterDataValidationException ex,
                                                                 HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiError> handleDownstream(DownstreamServiceException ex,
                                                      HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                           HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "OUTWARD_INVALID_REQUEST", ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        return ResponseEntity.<ApiError>status(status)
                .body(new ApiError(code, message, request.getHeader("X-Trace-Id"), Instant.now()));
    }
}
