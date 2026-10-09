package com.recnaile.notification.api;

import com.recnaile.notification.api.dto.NotificationResponse;
import com.recnaile.notification.application.NotificationInboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** User inbox API. The gateway/security layer must authenticate requests and set Principal.getName() to the employee ID (empId). */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationInboxController {
    private final NotificationInboxService inboxService;

    public NotificationInboxController(NotificationInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @Operation(summary = "List the authenticated employee's notifications",
            description = "Returns the authenticated employee's latest notifications using the service's fixed default result window. No query parameters are accepted.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT / employee identity")
    })
    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> listMine(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(inboxService.listForRecipient(principal.getName(), false, 50, 0));
    }

    @Operation(summary = "Get the authenticated employee's unread notification count")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread count retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT / employee identity")
    })
    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("unreadCount", inboxService.unreadCount(principal.getName())));
    }

    @Operation(summary = "Mark one notification as read",
            description = "Updates a notification belonging to the authenticated employee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT / employee identity"),
            @ApiResponse(responseCode = "404", description = "Notification does not exist or is not available to the caller")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(Principal principal,
                                                           @PathVariable String notificationId) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(inboxService.markRead(principal.getName(), notificationId));
    }

    @Operation(summary = "Mark all notifications for the authenticated employee as read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications updated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT / employee identity")
    })
    @PatchMapping("/me/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("updatedCount", inboxService.markAllRead(principal.getName())));
    }

    @Operation(summary = "Delete all notifications for the authenticated employee",
            description = "Deletes only the caller's notification inbox entries.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT / employee identity")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Integer>> deleteAllMine(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("deletedCount", inboxService.deleteAllForRecipient(principal.getName())));
    }
}
