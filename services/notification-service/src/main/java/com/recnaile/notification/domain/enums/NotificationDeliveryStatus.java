package com.recnaile.notification.domain.enums;

/** Delivery lifecycle, separate from whether the recipient has read the notification. */
public enum NotificationDeliveryStatus {
    PENDING,
    PROCESSING,
    SENT,
    RETRYING,
    FAILED,
    DEAD_LETTERED
}
