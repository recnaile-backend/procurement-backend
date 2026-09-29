package com.recnaile.outward.controller;

import com.recnaile.outward.dto.OutwardIssueTransactionResponse;
import com.recnaile.outward.dto.ApiError;
import com.recnaile.outward.service.OutwardIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outward")
@Tag(name = "Outward Issue", description = "Issue approved outward requests with optional idempotency protection.")
@SecurityRequirement(name = "bearerAuth")
public class OutwardIssueController {
    private final OutwardIssueService service;
    public OutwardIssueController(OutwardIssueService service) { this.service = service; }

    @Operation(summary = "Issue an approved outward request", description = "Creates the issue transaction and changes the outward request from APPROVED to ISSUED. Idempotency-Key is optional; when supplied, retries with the same key are replay-safe for the same outward request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue transaction created", content = @Content(schema = @Schema(implementation = OutwardIssueTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or idempotency key", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Outward request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not issuable, already issued, concurrently modified, or idempotency key reused", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/{outwardNumber}/issue")
    public ResponseEntity<OutwardIssueTransactionResponse> issue(
            @Parameter(description = "URL-encoded outward number stored by the service, for example OUT%2F26-27%2F001", required = true)
            @PathVariable String outwardNumber,
            @Parameter(in = ParameterIn.HEADER, description = "Optional client idempotency key. Reusing the same key for a different outward request is rejected.", required = false, example = "issue-OUT-001-20260929")
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.issue(OutwardNumberPath.normalize(outwardNumber), idempotencyKey, authentication));
    }
}
