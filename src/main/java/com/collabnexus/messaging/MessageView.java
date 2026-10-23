package com.collabnexus.messaging;

import java.time.Instant;

public record MessageView(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String senderRole,
        String body,
        boolean read,
        Instant createdAt
) {

    static MessageView from(ConversationMessage message) {
        return new MessageView(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getSender().getRole().name(),
                message.getBody(),
                message.getReadAt() != null,
                message.getCreatedAt()
        );
    }
}
