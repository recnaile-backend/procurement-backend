package com.recnaile.notification.application;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.model.NotificationDocument;
import com.recnaile.notification.persistence.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationInboxServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationInboxService service = new NotificationInboxService(repository);

    @Test
    void listsOnlyUnreadWhenRequestedAndCapsResults() {
        NotificationDocument notification = notification("user-1");
        when(repository.findByRecipientIdAndReadOrderByCreatedAtDesc("user-1", false))
                .thenReturn(List.of(notification));
        assertEquals(1, service.listForRecipient("user-1", true, 10).size());
    }

    @Test
    void returnsUnreadCountForAuthenticatedRecipient() {
        when(repository.countByRecipientIdAndReadFalse("user-1")).thenReturn(3L);

        assertEquals(3L, service.unreadCount("user-1"));
        verify(repository).countByRecipientIdAndReadFalse("user-1");
    }

    @Test
    void unreadCountRequiresRecipientIdentity() {
        assertThrows(ResponseStatusException.class, () -> service.unreadCount(" "));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsLimitsAboveMaximum() {
        assertThrows(ResponseStatusException.class, () -> service.listForRecipient("user-1", false, 101));
    }

    @Test
    void cannotMarkAnotherRecipientsNotificationRead() {
        when(repository.findByIdAndRecipientId("notification-1", "user-1"))
                .thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.markRead("user-1", "notification-1"));
        assertEquals(404, exception.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void marksAllUnreadNotificationsAndReturnsCount() {
        NotificationDocument first = notification("user-1");
        NotificationDocument second = notification("user-1");
        when(repository.findByRecipientIdAndReadOrderByCreatedAtDesc("user-1", false))
                .thenReturn(List.of(first, second));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(2, service.markAllRead("user-1"));
        assertTrue(first.isRead());
        assertTrue(second.isRead());
        verify(repository).saveAll(anyList());
    }

    @Test
    void deletesAllNotificationsOnlyForRecipientAndReturnsCount() {
        NotificationDocument first = notification("user-1");
        NotificationDocument second = notification("user-1");
        when(repository.findByRecipientIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(first, second));

        assertEquals(2, service.deleteAllForRecipient("user-1"));

        verify(repository).deleteAll(List.of(first, second));
        verify(repository, never()).deleteAll();
    }

    @Test
    void deletingAllNotificationsForEmptyInboxReturnsZero() {
        when(repository.findByRecipientIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of());

        assertEquals(0, service.deleteAllForRecipient("user-1"));

        verify(repository, never()).deleteAll(anyList());
    }

    @Test
    void deleteAllRequiresAuthenticatedRecipient() {
        assertThrows(ResponseStatusException.class, () -> service.deleteAllForRecipient(" "));
        verifyNoInteractions(repository);
    }

    private NotificationDocument notification(String recipient) {
        NotificationDocument document = new NotificationDocument("event-1", "INDENT_SUBMITTED", recipient,
                null, NotificationChannel.IN_APP, "Title", "Message", null, null);
        document.setId("notification-1");
        return document;
    }
}
