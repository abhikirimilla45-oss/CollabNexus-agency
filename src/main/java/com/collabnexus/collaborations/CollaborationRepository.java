package com.collabnexus.collaborations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {

    Optional<Collaboration> findByApplicationId(Long applicationId);

    Page<Collaboration> findByBrandId(Long brandId, Pageable pageable);

    Page<Collaboration> findByCreatorId(Long creatorId, Pageable pageable);

    Page<Collaboration> findByBrandIdAndStatus(Long brandId, CollaborationStatus status, Pageable pageable);

    Page<Collaboration> findByCreatorIdAndStatus(Long creatorId, CollaborationStatus status, Pageable pageable);

    Page<Collaboration> findByStatus(CollaborationStatus status, Pageable pageable);

    Optional<Collaboration> findByIdAndBrandId(Long id, Long brandId);

    Optional<Collaboration> findByIdAndCreatorId(Long id, Long creatorId);

    Optional<Collaboration> findByIdAndBrandUserId(Long id, Long userId);

    Optional<Collaboration> findByIdAndCreatorUserId(Long id, Long userId);

    long countByBrandIdAndStatus(Long brandId, CollaborationStatus status);

    long countByBrandId(Long brandId);

    List<Collaboration> findByBrandIdOrderByCreatedAtDesc(Long brandId);

    List<Collaboration> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    long countByCreatorIdAndStatus(Long creatorId, CollaborationStatus status);

    long countByCreatorId(Long creatorId);

    long countByStatus(CollaborationStatus status);
}
