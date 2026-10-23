package com.collabnexus.messaging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByCollaborationId(Long collaborationId);

    Page<Conversation> findByBrandUserId(Long userId, Pageable pageable);

    Page<Conversation> findByCreatorUserId(Long userId, Pageable pageable);

    Optional<Conversation> findByIdAndBrandUserId(Long id, Long userId);

    Optional<Conversation> findByIdAndCreatorUserId(Long id, Long userId);
}
