package com.recnaile.notification.application;

import com.recnaile.notification.api.dto.NotificationResponse;
import com.recnaile.notification.domain.model.NotificationDocument;
import com.recnaile.notification.persistence.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationInboxService {
    private final NotificationRepository repository;

    public NotificationInboxService(NotificationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<NotificationResponse> listForRecipient(String recipientId, Boolean unreadOnly, int limit) {
        requireRecipient(recipientId);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
        List<NotificationDocument> documents = Boolean.TRUE.equals(unreadOnly)
                ? repository.findByRecipientIdAndReadOrderByCreatedAtDesc(recipientId, false)
                : repository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        return documents.stream().limit(limit).map(NotificationResponse::from).toList();
    }

    public NotificationResponse markRead(String recipientId, String notificationId) {
        requireRecipient(recipientId);
        if (notificationId == null || notificationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notificationId must not be blank");
        }
        NotificationDocument notification = repository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead(Instant.now());
        return NotificationResponse.from(repository.save(notification));
    }

    public int markAllRead(String recipientId) {
        requireRecipient(recipientId);
        List<NotificationDocument> unread = repository.findByRecipientIdAndReadOrderByCreatedAtDesc(recipientId, false);
        Instant now = Instant.now();
        unread.forEach(notification -> notification.markRead(now));
        if (!unread.isEmpty()) repository.saveAll(unread);
        return unread.size();
    }

    /** Deletes every notification owned by the authenticated recipient, and no other recipient's data. */
    public int deleteAllForRecipient(String recipientId) {
        requireRecipient(recipientId);
        List<NotificationDocument> notifications = repository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        if (notifications.isEmpty()) return 0;
        repository.deleteAll(notifications);
        return notifications.size();
    }

    private static void requireRecipient(String recipientId) {
        if (recipientId == null || recipientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated recipient is required");
        }
    }
}
