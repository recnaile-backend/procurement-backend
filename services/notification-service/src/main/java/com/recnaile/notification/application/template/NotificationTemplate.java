package com.recnaile.notification.application.template;

import java.util.Objects;

/** Immutable title/body template for a supported domain event. */
public record NotificationTemplate(String title, String message) {
    public NotificationTemplate {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
        Objects.requireNonNull(title);
        Objects.requireNonNull(message);
    }
}
