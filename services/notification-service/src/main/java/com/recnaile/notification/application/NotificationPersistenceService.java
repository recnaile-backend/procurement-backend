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
     * Persists one notification per (eventId, empId, channel). The unique MongoDB index
     * is the concurrency-safe guard; the pre-check avoids unnecessary duplicate-key exceptions.
     */
    public NotificationDocument saveIfAbsent(NotificationDocument notification) {
        Objects.requireNonNull(notification, "notification");
        Optional<NotificationDocument> existing = repository.findByEventIdAndEmpIdAndChannel(
                notification.getEventId(), notification.getEmpId(), notification.getChannel());
        if (existing.isPresent()) return existing.get();
        try {
            return repository.save(notification);
        } catch (DuplicateKeyException duplicate) {
            return repository.findByEventIdAndEmpIdAndChannel(
                    notification.getEventId(), notification.getEmpId(), notification.getChannel())
                    .orElseThrow(() -> duplicate);
        }
    }

    public Optional<NotificationDocument> findByEventRecipientAndChannel(
            String eventId, String empId, NotificationChannel channel) {
        return repository.findByEventIdAndEmpIdAndChannel(eventId, empId, channel);
    }
}
