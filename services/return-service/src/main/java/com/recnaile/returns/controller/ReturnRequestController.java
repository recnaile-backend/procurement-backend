package com.recnaile.returns.controller;

import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.dto.VerifyReturnRequest;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.service.ReturnRequestService;
import com.recnaile.returns.service.ReturnProcessingService;
import com.recnaile.returns.service.ReturnVerificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@Tag(name = "Returns", description = "Return creation, verification and processing APIs")
@RequestMapping("/api/v1/returns")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;
    private final ReturnVerificationService returnVerificationService;
    private final ReturnProcessingService returnProcessingService;

    public ReturnRequestController(
            ReturnRequestService returnRequestService,
            ReturnVerificationService returnVerificationService,
            ReturnProcessingService returnProcessingService) {
        this.returnRequestService = returnRequestService;
        this.returnVerificationService = returnVerificationService;
        this.returnProcessingService = returnProcessingService;
    }

    @GetMapping
    @Operation(
            summary = "Get all returns",
            description = "Returns all return requests, or a single return when returnId is supplied. Access is restricted to TL, MD and PROCUREMENT roles."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    })
    @PreAuthorize("hasAnyRole('TL', 'MD', 'PROCUREMENT')")
    public ResponseEntity<ApiResponse<List<ReturnRequest>>> getAll(
            @Parameter(name = "returnId", description = "Optional return ID filter. When omitted, all returns are returned.", required = false, example = "RETURN%2F26-27%2F001")
            @RequestParam(value = "returnId", required = false) String returnId) {
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.getAll(returnId)));
    }

    @PostMapping
    @Operation(summary = "Create a return", description = "Creates a return request after validating the issued outward item and records it as pending verification.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Return created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = com.recnaile.returns.api.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'PROCUREMENT', 'TL', 'MD')")
    public ResponseEntity<ApiResponse<ReturnRequest>> create(
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, description = "Optional retry key for safe repeat submission", required = false, example = "return-create-123")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication,
            @Valid @RequestBody CreateReturnRequest request) {
        ReturnRequest created = returnRequestService.create(request, authenticatedEmployeeId(authentication), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PostMapping("/{returnId}/verify")
    @Operation(summary = "Verify a return", description = "Verifies a pending return using the authenticated procurement/TL/MD actor.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Return verified"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or state transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Return not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'TL', 'MD')")
    public ResponseEntity<ApiResponse<ReturnRequest>> verify(
            @Parameter(name = "returnId", in = ParameterIn.PATH, description = "Return ID", required = true)
            @PathVariable String returnId,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, description = "Optional retry key for safe repeat verification", required = false, example = "return-verify-123")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication,
            @Valid @RequestBody VerifyReturnRequest request) {
        ReturnRequest verified = returnVerificationService.verify(ReturnNumberPath.normalize(returnId), request, authenticatedEmployeeId(authentication), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(verified));
    }

    @PostMapping("/{returnId}/process")
    @Operation(summary = "Process a verified return", description = "Processes a verified return and records the downstream return event through the transactional outbox.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Return processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or state transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Return not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'TL', 'MD')")
    public ResponseEntity<ApiResponse<ReturnRequest>> process(
            @Parameter(name = "returnId", in = ParameterIn.PATH, description = "Return ID", required = true)
            @PathVariable String returnId,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, description = "Optional retry key for safe repeat processing", required = false, example = "return-process-123")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        ReturnRequest processed = returnProcessingService.process(ReturnNumberPath.normalize(returnId), authenticatedEmployeeId(authentication), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(processed));
    }
    /**
     * The Auth Service puts the business employee identifier in the JWT
     * employeeId claim. The MongoDB user _id (JWT subject) is intentionally
     * not used as the Return Service business employee identifier.
     */
    private String authenticatedEmployeeId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String employeeId = jwt.getClaimAsString("employeeId");
            if (employeeId == null || employeeId.isBlank()) {
                throw new IllegalArgumentException("Authenticated JWT does not contain employeeId");
            }
            return employeeId.trim();
        }

        // Keeps controller unit tests based on @WithMockUser deterministic.
        String employeeId = authentication.getName();
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Authenticated employeeId must not be blank");
        }
        return employeeId.trim();
    }

}
