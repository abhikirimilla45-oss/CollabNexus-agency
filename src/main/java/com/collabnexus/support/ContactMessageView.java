package com.collabnexus.support;

import java.time.Instant;

public record ContactMessageView(
        Long id,
        String name,
        String email,
        ContactRole role,
        ContactTopic topic,
        String message,
        ContactStatus status,
        Instant createdAt,
        Instant handledAt,
        Long handledBy
) {

    public static ContactMessageView from(ContactMessage contactMessage) {
        return new ContactMessageView(
                contactMessage.getId(),
                contactMessage.getName(),
                contactMessage.getEmail(),
                contactMessage.getRole(),
                contactMessage.getTopic(),
                contactMessage.getMessage(),
                contactMessage.getStatus(),
                contactMessage.getCreatedAt(),
                contactMessage.getHandledAt(),
                contactMessage.getHandledBy()
        );
    }
}
