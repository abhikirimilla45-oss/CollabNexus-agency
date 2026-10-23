package com.collabnexus.reviews;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByCollaborationIdAndAuthorId(Long collaborationId, Long authorId);

    Optional<Review> findByCollaborationIdAndAuthorId(Long collaborationId, Long authorId);

    Page<Review> findByAuthorId(Long authorId, Pageable pageable);

    Page<Review> findBySubjectIdAndStatus(Long subjectId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    long countByStatus(ReviewStatus status);

    long countBySubjectIdAndStatus(Long subjectId, ReviewStatus status);

    long countBySubjectId(Long subjectId);
}
