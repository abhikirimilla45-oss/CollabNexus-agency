package com.collabnexus.reviews;

import java.time.Instant;

public record ReviewView(
        Long id,
        Long collaborationId,
        String campaignTitle,
        Long authorId,
        String authorName,
        String authorRole,
        Long subjectId,
        String subjectName,
        int rating,
        String comment,
        ReviewStatus status,
        Instant createdAt
) {

    static ReviewView from(Review review) {
        return new ReviewView(
                review.getId(),
                review.getCollaboration().getId(),
                review.getCollaboration().getCampaign().getTitle(),
                review.getAuthor().getId(),
                review.getAuthor().getFullName(),
                review.getAuthor().getRole().name(),
                review.getSubject().getId(),
                review.getSubject().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt()
        );
    }
}
