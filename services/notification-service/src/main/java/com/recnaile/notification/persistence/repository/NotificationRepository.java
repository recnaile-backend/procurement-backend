package com.recnaile.notification.persistence.repository;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.model.NotificationDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    boolean existsByEventIdAndEmpIdAndChannel(String eventId, String empId, NotificationChannel channel);
    Optional<NotificationDocument> findByEventIdAndEmpIdAndChannel(String eventId, String empId, NotificationChannel channel);
    List<NotificationDocument> findByEmpIdOrderByCreatedAtDesc(String empId);
    List<NotificationDocument> findByEmpId(String empId, Pageable pageable);
    List<NotificationDocument> findByEmpIdAndReadOrderByCreatedAtDesc(String empId, boolean read);
    List<NotificationDocument> findByEmpIdAndRead(String empId, boolean read, Pageable pageable);
    long countByEmpIdAndReadFalse(String empId);
    Optional<NotificationDocument> findByIdAndEmpId(String id, String empId);
}
