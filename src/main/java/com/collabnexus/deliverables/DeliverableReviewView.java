package com.collabnexus.deliverables;

import java.time.Instant;

public record DeliverableReviewView(
        Long id,
        DeliverableReviewAction action,
        String comments,
        String reviewerName,
        Instant createdAt
) {

    public static DeliverableReviewView from(DeliverableReview review) {
        return new DeliverableReviewView(
                review.getId(),
                review.getAction(),
                review.getComments(),
                review.getReviewer().getFullName(),
                review.getCreatedAt()
        );
    }
}
