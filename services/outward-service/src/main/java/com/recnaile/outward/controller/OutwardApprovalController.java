package com.recnaile.outward.controller;

import com.recnaile.outward.dto.OutwardResponse;
import com.recnaile.outward.dto.RejectOutwardRequest;
import com.recnaile.outward.service.OutwardApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
public class OutwardApprovalController {

    private final OutwardApprovalService service;

    public OutwardApprovalController(OutwardApprovalService service) {
        this.service = service;
    }

    @Operation(summary = "Submit an outward request for approval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request submitted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Requester authorization failed"),
            @ApiResponse(responseCode = "409", description = "Invalid state or concurrent modification")
    })
    @PostMapping("/{outwardNumber}/submit")
    public ResponseEntity<OutwardResponse> submit(@PathVariable String outwardNumber,
                                                    Authentication authentication) {
        return ResponseEntity.ok(service.submit(outwardNumber, requireAuthentication(authentication)));
    }

    @Operation(summary = "Approve an outward request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request approved"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Approval role not allowed"),
            @ApiResponse(responseCode = "409", description = "Invalid state or concurrent modification")
    })
    @PostMapping("/{outwardNumber}/approve")
    public ResponseEntity<OutwardResponse> approve(@PathVariable String outwardNumber,
                                                     Authentication authentication) {
        return ResponseEntity.ok(service.approve(outwardNumber, requireAuthentication(authentication)));
    }

    @Operation(summary = "Reject an outward request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected"),
            @ApiResponse(responseCode = "400", description = "Invalid rejection request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Approval role not allowed"),
            @ApiResponse(responseCode = "409", description = "Invalid state or concurrent modification")
    })
    @PostMapping("/{outwardNumber}/reject")
    public ResponseEntity<OutwardResponse> reject(@PathVariable String outwardNumber,
                                                    @Valid @RequestBody(required = false) RejectOutwardRequest request,
                                                    Authentication authentication) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(service.reject(outwardNumber, reason, requireAuthentication(authentication)));
    }

    private Authentication requireAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "AUTHENTICATION_REQUIRED", "Authenticated user is required");
        }
        return authentication;
    }
}
