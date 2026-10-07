package com.recnaile.outward.exception;

import com.recnaile.outward.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.recnaile.outward.service.InvalidOutwardStateTransitionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OutwardExceptionHandler {


    @ExceptionHandler(OutwardException.class)
    public ResponseEntity<ApiError> handleOutward(OutwardException ex, HttpServletRequest request) {
        HttpStatus status = switch (ex.getCode()) {
            case "OUTWARD_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "OUTWARD_APPROVAL_ROLE_NOT_ALLOWED", "OUTWARD_APPROVAL_DEPARTMENT_REQUIRED",
                    "OUTWARD_APPROVAL_DEPARTMENT_MISMATCH", "OUTWARD_ISSUE_ROLE_NOT_ALLOWED" -> HttpStatus.FORBIDDEN;
            case "AUTHENTICATION_REQUIRED" -> HttpStatus.UNAUTHORIZED;
            case "AUTH_EMPLOYEE_LOOKUP_FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "OUTWARD_CONCURRENT_MODIFICATION", "OUTWARD_NOT_APPROVED", "OUTWARD_ALREADY_ISSUED",
                    "OUTWARD_IDEMPOTENCY_KEY_REUSED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return error(status, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidOutwardStateTransitionException.class)
    public ResponseEntity<ApiError> handleStateTransition(InvalidOutwardStateTransitionException ex,
                                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), request);
    }

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
