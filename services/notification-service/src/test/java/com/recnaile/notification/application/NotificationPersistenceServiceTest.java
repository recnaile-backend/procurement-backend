package com.recnaile.notification.application;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.model.NotificationDocument;
import com.recnaile.notification.persistence.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationPersistenceServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationPersistenceService service = new NotificationPersistenceService(repository);

    @Test
    void returnsExistingNotificationForDuplicateEventRecipientAndChannel() {
        NotificationDocument existing = notification();
        when(repository.findByEventIdAndEmpIdAndChannel("event-1", "EMP-1", NotificationChannel.IN_APP))
                .thenReturn(Optional.of(existing));

        assertSame(existing, service.saveIfAbsent(notification()));
        verify(repository, never()).save(any());
    }

    @Test
    void savesNotificationWhenNoDuplicateExists() {
        NotificationDocument candidate = notification();
        when(repository.findByEventIdAndEmpIdAndChannel("event-1", "EMP-1", NotificationChannel.IN_APP))
                .thenReturn(Optional.empty());
        when(repository.save(candidate)).thenReturn(candidate);

        assertSame(candidate, service.saveIfAbsent(candidate));
        verify(repository).save(candidate);
    }

    @Test
    void resolvesConcurrentDuplicateUsingUniqueIndexLookup() {
        NotificationDocument existing = notification();
        when(repository.findByEventIdAndEmpIdAndChannel("event-1", "EMP-1", NotificationChannel.IN_APP))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(repository.save(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertSame(existing, service.saveIfAbsent(notification()));
    }

    private NotificationDocument notification() {
        return new NotificationDocument("event-1", "INDENT_SUBMITTED", "EMP-1", null,
                NotificationChannel.IN_APP, "Title", "Message", null, null);
    }
}
