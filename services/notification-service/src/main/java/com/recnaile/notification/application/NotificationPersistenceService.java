package com.recnaile.notification.application;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.model.NotificationDocument;
import com.recnaile.notification.persistence.repository.NotificationRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/** Persistence boundary with duplicate protection backed by a MongoDB unique index. */
@Service
public class NotificationPersistenceService {
    private final NotificationRepository repository;

    public NotificationPersistenceService(NotificationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Persists one notification per (eventId, recipientId, channel). The unique MongoDB index
     * is the concurrency-safe guard; the pre-check avoids unnecessary duplicate-key exceptions.
     */
    public NotificationDocument saveIfAbsent(NotificationDocument notification) {
        Objects.requireNonNull(notification, "notification");
        Optional<NotificationDocument> existing = repository.findByEventIdAndRecipientIdAndChannel(
                notification.getEventId(), notification.getRecipientId(), notification.getChannel());
        if (existing.isPresent()) return existing.get();
        try {
            return repository.save(notification);
        } catch (DuplicateKeyException duplicate) {
            return repository.findByEventIdAndRecipientIdAndChannel(
                    notification.getEventId(), notification.getRecipientId(), notification.getChannel())
                    .orElseThrow(() -> duplicate);
        }
    }

    public Optional<NotificationDocument> findByEventRecipientAndChannel(
            String eventId, String recipientId, NotificationChannel channel) {
        return repository.findByEventIdAndRecipientIdAndChannel(eventId, recipientId, channel);
    }
}
