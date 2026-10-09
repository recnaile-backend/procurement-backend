package com.recnaile.notification.api.operations;

import com.recnaile.notification.application.replay.AuditedEventReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

/** Operational replay endpoint. Only the Managing Director (MD) authority may invoke it. */
@RestController
@RequestMapping("/api/v1/notifications/operations/events")
public class NotificationReplayController {
    private static final Set<String> ALLOWED = Set.of("ROLE_MD");
    private final AuditedEventReplayService replayService;

    public NotificationReplayController(AuditedEventReplayService replayService) {
        this.replayService = replayService;
    }

    @Operation(summary = "Replay a failed domain event",
            description = "Privileged operation. Requires the ROLE_MD authority issued by the Auth Service. "
                    + "Replay attempts integrate with the separate Audit Service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Replay request processed; inspect response for replay result"),
            @ApiResponse(responseCode = "400", description = "Event input or state is invalid for replay"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Caller lacks an allowed replay authority"),
            @ApiResponse(responseCode = "404", description = "Event was not found"),
            @ApiResponse(responseCode = "503", description = "Audit event could not be published; replay was not confirmed")
    })
    @PostMapping("/{eventId}/replay")
    public AuditedEventReplayService.ReplayResult replay(@PathVariable String eventId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (authorities.stream().noneMatch(ALLOWED::contains)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MD authority required for event replay");
        }
        try {
            return replayService.replay(eventId, authentication.getName());
        } catch (IllegalArgumentException missing) {
            if ("EVENT_NOT_FOUND".equals(missing.getMessage())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missing.getMessage());
        } catch (IllegalStateException auditFailure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audit event could not be published");
        }
    }
}
