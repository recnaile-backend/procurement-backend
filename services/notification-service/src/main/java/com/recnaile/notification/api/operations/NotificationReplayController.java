package com.recnaile.notification.api.operations;

import com.recnaile.notification.application.replay.AuditedEventReplayService;
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

/** Operational replay endpoint. Only explicit administrative authorities may invoke it. */
@RestController
@RequestMapping("/api/v1/notifications/operations/events")
public class NotificationReplayController {
    private static final Set<String> ALLOWED = Set.of("ROLE_ADMIN", "ROLE_AUDIT_ADMIN", "SCOPE_notification.replay");
    private final AuditedEventReplayService replayService;

    public NotificationReplayController(AuditedEventReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/{eventId}/replay")
    public AuditedEventReplayService.ReplayResult replay(@PathVariable String eventId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (authorities.stream().noneMatch(ALLOWED::contains)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrative replay authority required");
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
