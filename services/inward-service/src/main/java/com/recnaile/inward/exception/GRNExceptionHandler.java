package com.recnaile.inward.exception;

import com.recnaile.inward.dto.ApiErrorResponse;
import com.recnaile.inward.po.PurchaseOrderClientException;
import com.recnaile.inward.po.PurchaseOrderForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GRNExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String detail = rootMessage(ex);
        String message = "Request body is invalid or could not be parsed"
                + (detail.isBlank() ? "." : ": " + detail);
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required request parameter '" + ex.getParameterName() + "' is missing.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "'.", request);
    }

    @ExceptionHandler(GRNNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(GRNNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "GRN_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePurchaseOrderNotFound(
            PurchaseOrderNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateDeliveryNoteException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateDeliveryNote(
            DuplicateDeliveryNoteException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_DELIVERY_NOTE", ex.getMessage(), request);
    }

    @ExceptionHandler(PurchaseOrderNotEligibleException.class)
    public ResponseEntity<ApiErrorResponse> handlePurchaseOrderNotEligible(
            PurchaseOrderNotEligibleException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "PURCHASE_ORDER_NOT_ISSUED", ex.getMessage(), request);
    }

    @ExceptionHandler(ReceivedQuantityExceedsOrderedException.class)
    public ResponseEntity<ApiErrorResponse> handleReceivedQuantityExceedsOrdered(
            ReceivedQuantityExceedsOrderedException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "RECEIVED_QUANTITY_EXCEEDS_ORDERED", ex.getMessage(), request);
    }

    @ExceptionHandler(ItemNotInPurchaseOrderException.class)
    public ResponseEntity<ApiErrorResponse> handleItemNotInPurchaseOrder(
            ItemNotInPurchaseOrderException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "ITEM_NOT_IN_PURCHASE_ORDER", ex.getMessage(), request);
    }

    @ExceptionHandler(RejectionReasonRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleRejectionReasonRequired(
            RejectionReasonRequiredException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "REJECTION_REASON_REQUIRED", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateGRNItemException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateGRNItem(
            DuplicateGRNItemException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "DUPLICATE_ITEM_IN_GRN", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicatePurchaseOrderItemException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicatePurchaseOrderItem(
            DuplicatePurchaseOrderItemException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "DUPLICATE_ITEM_IN_PURCHASE_ORDER", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidGRNStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStateTransition(
            InvalidGRNStateTransitionException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "INVALID_GRN_STATE_TRANSITION", ex.getMessage(), request);
    }

    @ExceptionHandler(PurchaseOrderForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handlePurchaseOrderForbidden(
            PurchaseOrderForbiddenException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "PURCHASE_ORDER_ACCESS_FORBIDDEN",
                "You do not have permission to access the referenced Purchase Order " + ex.getMessage(), request);
    }

    @ExceptionHandler(PurchaseOrderClientException.class)
    public ResponseEntity<ApiErrorResponse> handlePurchaseOrderClient(
            PurchaseOrderClientException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, "PURCHASE_ORDER_SERVICE_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(com.recnaile.inward.exception.IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(
            com.recnaile.inward.exception.IdempotencyConflictException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(com.recnaile.inward.exception.IdempotencyInProgressException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyInProgress(
            com.recnaile.inward.exception.IdempotencyInProgressException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "IDEMPOTENCY_REQUEST_IN_PROGRESS", ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "GRN_VERSION_CONFLICT",
                "GRN was modified by another request. Please refresh and try again.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_INTEGRITY_ERROR",
                "The request conflicts with existing data. " + rootMessage(ex), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, resolveCode(ex.getMessage()),
                safeMessage(ex, "Invalid GRN request."), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Keep the standard envelope, but include the actual exception detail so that
        // Swagger/Postman users can diagnose server-side failures using the traceId.
        String detail = rootMessage(ex);
        String message = detail.isBlank()
                ? "An unexpected error occurred. Check the service logs using the traceId."
                : "An unexpected error occurred: " + detail;
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message, request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        String traceId = request.getHeader("X-Correlation-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return ResponseEntity.status(status).body(ApiErrorResponse.of(code,
                message == null || message.isBlank() ? "No additional error details available." : message,
                traceId));
    }

    private String resolveCode(String message) {
        if (message == null) return "INVALID_GRN_REQUEST";
        if (message.contains("does not belong to PO")) return "ITEM_NOT_IN_PURCHASE_ORDER";
        if (message.contains("contains duplicate itemName")) return "DUPLICATE_ITEM_IN_GRN";
        if (message.contains("rejectionReason is required")) return "REJECTION_REASON_REQUIRED";
        if (message.contains("exceeds ordered quantity")) return "RECEIVED_QUANTITY_EXCEEDS_ORDERED";
        return "INVALID_GRN_REQUEST";
    }

    private String safeMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable last = throwable;
        while (current != null) {
            last = current;
            current = current.getCause();
        }
        String message = last.getMessage();
        return message == null ? "" : message.trim();
    }
}
