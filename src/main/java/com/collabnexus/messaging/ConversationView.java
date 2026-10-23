package com.collabnexus.messaging;

import java.time.Instant;

public record ConversationView(
        Long id,
        Long collaborationId,
        String campaignTitle,
        String brandName,
        String creatorName,
        String subject,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount,
        Instant updatedAt
) {
}
