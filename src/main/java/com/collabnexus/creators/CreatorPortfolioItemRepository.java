package com.collabnexus.creators;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorPortfolioItemRepository extends JpaRepository<CreatorPortfolioItem, Long> {

    List<CreatorPortfolioItem> findByCreatorIdOrderByCreatedAtAsc(Long creatorId);

    Optional<CreatorPortfolioItem> findByIdAndCreatorUserId(Long id, Long userId);
}
