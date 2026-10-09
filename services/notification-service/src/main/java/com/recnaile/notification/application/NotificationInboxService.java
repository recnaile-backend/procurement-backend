package com.recnaile.notification.application;

import com.recnaile.notification.api.dto.NotificationResponse;
import com.recnaile.notification.domain.model.NotificationDocument;
import com.recnaile.notification.persistence.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public List<NotificationResponse> listForRecipient(String empId, Boolean unreadOnly, int limit) {
        requireRecipient(empId);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
        List<NotificationDocument> documents = Boolean.TRUE.equals(unreadOnly)
                ? repository.findByEmpIdAndReadOrderByCreatedAtDesc(empId, false)
                : repository.findByEmpIdOrderByCreatedAtDesc(empId);
        return documents.stream().limit(limit).map(NotificationResponse::from).toList();
    }

    /**
     * Lists a single bounded page from MongoDB, avoiding loading the recipient's full inbox into memory.
     * Page numbering is zero-based and results are sorted newest-first with a stable ID tie-breaker.
     */
    public List<NotificationResponse> listForRecipient(String empId, Boolean unreadOnly, int limit, int page) {
        requireRecipient(empId);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
        if (page < 0 || page > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be between 0 and 100000");
        }
        PageRequest pageable = PageRequest.of(page, limit,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        List<NotificationDocument> documents = Boolean.TRUE.equals(unreadOnly)
                ? repository.findByEmpIdAndRead(empId, false, pageable)
                : repository.findByEmpId(empId, pageable);
        return documents.stream().map(NotificationResponse::from).toList();
    }

    /** Returns the number of unread notifications belonging to the recipient. */
    public long unreadCount(String empId) {
        requireRecipient(empId);
        return repository.countByEmpIdAndReadFalse(empId);
    }

    public NotificationResponse markRead(String empId, String notificationId) {
        requireRecipient(empId);
        if (notificationId == null || notificationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notificationId must not be blank");
        }
        NotificationDocument notification = repository.findByIdAndEmpId(notificationId, empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead(Instant.now());
        return NotificationResponse.from(repository.save(notification));
    }

    public int markAllRead(String empId) {
        requireRecipient(empId);
        List<NotificationDocument> unread = repository.findByEmpIdAndReadOrderByCreatedAtDesc(empId, false);
        Instant now = Instant.now();
        unread.forEach(notification -> notification.markRead(now));
        if (!unread.isEmpty()) repository.saveAll(unread);
        return unread.size();
    }

    /** Deletes every notification owned by the authenticated recipient, and no other recipient's data. */
    public int deleteAllForRecipient(String empId) {
        requireRecipient(empId);
        List<NotificationDocument> notifications = repository.findByEmpIdOrderByCreatedAtDesc(empId);
        if (notifications.isEmpty()) return 0;
        repository.deleteAll(notifications);
        return notifications.size();
    }

    private static void requireRecipient(String empId) {
        if (empId == null || empId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated recipient is required");
        }
    }
}
