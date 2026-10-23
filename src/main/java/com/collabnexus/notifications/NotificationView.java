package com.collabnexus.notifications;

import java.time.Instant;

public record NotificationView(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        Instant readAt,
        Instant createdAt
) {

    static NotificationView from(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
