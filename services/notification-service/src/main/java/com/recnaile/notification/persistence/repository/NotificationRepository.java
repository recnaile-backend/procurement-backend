package com.recnaile.notification.persistence.repository;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.model.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    boolean existsByEventIdAndRecipientIdAndChannel(String eventId, String recipientId, NotificationChannel channel);
    Optional<NotificationDocument> findByEventIdAndRecipientIdAndChannel(String eventId, String recipientId, NotificationChannel channel);
    List<NotificationDocument> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    List<NotificationDocument> findByRecipientIdAndReadOrderByCreatedAtDesc(String recipientId, boolean read);
    Optional<NotificationDocument> findByIdAndRecipientId(String id, String recipientId);
}
