package com.collabnexus.creators;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorSocialChannelRepository extends JpaRepository<CreatorSocialChannel, Long> {

    List<CreatorSocialChannel> findByCreatorIdOrderByCreatedAtAsc(Long creatorId);

    Optional<CreatorSocialChannel> findByIdAndCreatorUserId(Long id, Long userId);
}
