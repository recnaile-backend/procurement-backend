package com.recnaile.notification.domain.model;

import com.recnaile.notification.domain.enums.NotificationChannel;
import com.recnaile.notification.domain.enums.NotificationDeliveryStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDocumentTest {
    @Test
    void newNotificationStartsPendingAndUnread() {
        NotificationDocument notification = new NotificationDocument(
                "event-1", "INDENT_SUBMITTED", "EMP-1", "PROCUREMENT",
                NotificationChannel.IN_APP, "Indent submitted", "Indent I-100 was submitted", null, "trace-1");

        assertEquals(NotificationDeliveryStatus.PENDING, notification.getDeliveryStatus());
        assertFalse(notification.isRead());
        assertEquals(0, notification.getDeliveryAttempts());
        assertNotNull(notification.getCreatedAt());
        assertEquals("event-1", notification.getEventId());
    }

    @Test
    void readStateIsIndependentFromDeliveryState() {
        NotificationDocument notification = new NotificationDocument(
                "event-1", "INDENT_SUBMITTED", "EMP-1", null,
                NotificationChannel.IN_APP, "Title", "Message", null, null);
        Instant readAt = Instant.parse("2026-10-09T10:00:00Z");

        notification.markRead(readAt);

        assertTrue(notification.isRead());
        assertEquals(readAt, notification.getReadAt());
        assertEquals(NotificationDeliveryStatus.PENDING, notification.getDeliveryStatus());
    }

    @Test
    void deliveryAttemptsAndStatusCanAdvance() {
        NotificationDocument notification = new NotificationDocument(
                "event-1", "PO_ISSUED", "EMP-1", null,
                NotificationChannel.EMAIL, "PO issued", "PO PO-100 was issued", "corr-1", "trace-1");
        Instant now = Instant.parse("2026-10-09T10:00:00Z");

        notification.recordAttempt(now);
        notification.markDeliveryStatus(NotificationDeliveryStatus.SENT, now);

        assertEquals(1, notification.getDeliveryAttempts());
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveryStatus());
        assertEquals("corr-1", notification.getCorrelationId());
    }

    @Test
    void requiredIdentityAndMessageFieldsCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new NotificationDocument(
                " ", "INDENT_SUBMITTED", "EMP-1", null,
                NotificationChannel.IN_APP, "Title", "Message", null, null));
    }
    @Test
    void nullChannelIsRejected() {
        assertThrows(NullPointerException.class, () -> new NotificationDocument(
                "event-1", "INDENT_SUBMITTED", "EMP-1", null, null,
                "Title", "Message", null, null));
    }

    @Test
    void retrySchedulingRecordsRetryStateAndTime() {
        NotificationDocument notification = new NotificationDocument(
                "event-2", "PO_ISSUED", "user-2", null,
                NotificationChannel.EMAIL, "PO issued", "PO-100 issued", null, null);
        Instant retryAt = Instant.parse("2026-10-10T10:00:00Z");

        notification.scheduleRetry(retryAt);

        assertEquals(NotificationDeliveryStatus.RETRYING, notification.getDeliveryStatus());
        assertEquals(retryAt, notification.getNextAttemptAt());
        assertNotNull(notification.getUpdatedAt());
    }

    @Test
    void repeatedMarkReadDoesNotOverwriteOriginalReadTimestamp() {
        NotificationDocument notification = new NotificationDocument(
                "event-3", "INDENT_VERIFIED", "user-3", null,
                NotificationChannel.IN_APP, "Verified", "Indent verified", null, null);
        Instant first = Instant.parse("2026-10-09T10:00:00Z");
        Instant second = Instant.parse("2026-10-09T11:00:00Z");

        notification.markRead(first);
        notification.markRead(second);

        assertEquals(first, notification.getReadAt());
    }

}
