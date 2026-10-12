package com.recnaile.audit.api;

import com.recnaile.audit.application.AuditEventReplayService;
import com.recnaile.audit.security.AuditAccessPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operational replay endpoint; restricted to MD and never reads or deletes the DLT automatically. */
@RestController
@RequestMapping("/api/v1/audit-events/operations")
@Tag(name = "Audit Operations", description = "Restricted operational controls")
public class AuditReplayController {
    private final AuditEventReplayService replayService;
    private final AuditAccessPolicy accessPolicy;

    public AuditReplayController(AuditEventReplayService replayService, AuditAccessPolicy accessPolicy) {
        this.replayService = replayService;
        this.accessPolicy = accessPolicy;
    }

    @PostMapping("/replay")
    @Operation(summary = "Validate and republish one failed AUDIT_EVENT envelope; MD only")
    public ResponseEntity<ApiResponse<AuditEventReplayService.ReplayReceipt>> replay(
            @Valid @RequestBody ReplayRequest request, @AuthenticationPrincipal Jwt jwt) {
        accessPolicy.requireMdOperations(jwt);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(replayService.replay(request.rawEventJson())));
    }

    public record ReplayRequest(@NotBlank String rawEventJson) { }
}
