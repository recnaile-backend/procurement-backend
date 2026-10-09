package com.recnaile.notification.api;

import com.recnaile.notification.api.dto.NotificationResponse;
import com.recnaile.notification.application.NotificationInboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** User inbox API. The gateway/security layer must authenticate requests and set Principal.getName() to the stable user ID. */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationInboxController {
    private final NotificationInboxService inboxService;

    public NotificationInboxController(NotificationInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> listMine(
            Principal principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "50") int limit) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(inboxService.listForRecipient(principal.getName(), unreadOnly, limit));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("unreadCount", inboxService.unreadCount(principal.getName())));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(Principal principal,
                                                           @PathVariable String notificationId) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(inboxService.markRead(principal.getName(), notificationId));
    }

    @PatchMapping("/me/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("updatedCount", inboxService.markAllRead(principal.getName())));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Integer>> deleteAllMine(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("deletedCount", inboxService.deleteAllForRecipient(principal.getName())));
    }
}
