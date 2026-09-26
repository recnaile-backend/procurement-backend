package com.recnaile.outward.controller;

import com.recnaile.outward.dto.OutwardIssueTransactionResponse;
import com.recnaile.outward.service.OutwardIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outward")
public class OutwardIssueController {

    private final OutwardIssueService service;

    public OutwardIssueController(OutwardIssueService service) {
        this.service = service;
    }

    @Operation(summary = "Issue an approved outward request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue transaction created"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Outward request not found"),
            @ApiResponse(responseCode = "409", description = "Request is not issuable, already issued, or concurrently modified")
    })
    @PostMapping("/{outwardNumber}/issue")
    public ResponseEntity<OutwardIssueTransactionResponse> issue(
            @PathVariable String outwardNumber, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.issue(outwardNumber, authentication));
    }
}
