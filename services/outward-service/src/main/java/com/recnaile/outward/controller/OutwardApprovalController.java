package com.recnaile.outward.controller;

import com.recnaile.outward.dto.OutwardResponse;
import com.recnaile.outward.dto.ApiError;
import com.recnaile.outward.dto.RejectOutwardRequest;
import com.recnaile.outward.service.OutwardApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outward")
@Tag(name = "Outward Approval", description = "Submit, approve and reject outward requests.")
@SecurityRequirement(name = "bearerAuth")
public class OutwardApprovalController {
    private final OutwardApprovalService service;
    public OutwardApprovalController(OutwardApprovalService service) { this.service = service; }

    @Operation(summary = "Submit an outward request for approval", description = "Changes a requester-owned DRAFT request to PENDING_APPROVAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request submitted", content = @Content(schema = @Schema(implementation = OutwardResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Requester authorization failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Outward request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state or concurrent modification", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{outwardNumber}/submit")
    public ResponseEntity<OutwardResponse> submit(@PathVariable String outwardNumber, Authentication authentication) {
        return ResponseEntity.ok(service.submit(OutwardNumberPath.normalize(outwardNumber), requireAuthentication(authentication)));
    }

    @Operation(summary = "Approve an outward request", description = "Changes a PENDING_APPROVAL request to APPROVED when the authenticated user has a configured approval role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request approved", content = @Content(schema = @Schema(implementation = OutwardResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Approval role not allowed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Outward request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state or concurrent modification", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{outwardNumber}/approve")
    public ResponseEntity<OutwardResponse> approve(@PathVariable String outwardNumber, Authentication authentication) {
        return ResponseEntity.ok(service.approve(OutwardNumberPath.normalize(outwardNumber), requireAuthentication(authentication)));
    }

    @Operation(summary = "Reject an outward request", description = "Changes a PENDING_APPROVAL request to REJECTED and records the rejection reason when supplied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected", content = @Content(schema = @Schema(implementation = OutwardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rejection request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Approval role not allowed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Outward request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state or concurrent modification", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{outwardNumber}/reject")
    public ResponseEntity<OutwardResponse> reject(
            @PathVariable String outwardNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional rejection reason", content = @Content(schema = @Schema(implementation = RejectOutwardRequest.class)))
            @Valid @RequestBody(required = false) RejectOutwardRequest request,
            Authentication authentication) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(service.reject(OutwardNumberPath.normalize(outwardNumber), reason, requireAuthentication(authentication)));
    }

    private Authentication requireAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new com.recnaile.outward.exception.OutwardException("AUTHENTICATION_REQUIRED", "Authenticated user is required");
        }
        return authentication;
    }
}
