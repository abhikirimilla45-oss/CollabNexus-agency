package com.collabnexus.messaging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    Page<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    Optional<ConversationMessage> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long userId);

    @Modifying
    @Query("update ConversationMessage message set message.readAt = :readAt "
            + "where message.conversation.id = :conversationId and message.sender.id <> :userId "
            + "and message.readAt is null")
    int markRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId,
                 @Param("readAt") Instant readAt);
}
