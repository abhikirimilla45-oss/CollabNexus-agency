package com.collabnexus.deliverables;

import com.collabnexus.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "deliverable_reviews",
        indexes = @Index(name = "idx_review_deliverable", columnList = "deliverable_id"))
public class DeliverableReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deliverable_id", nullable = false)
    private Deliverable deliverable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private DeliverableSubmission submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliverableReviewAction action;

    @Column(length = 2000)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DeliverableReview() {
    }

    public DeliverableReview(Deliverable deliverable, DeliverableSubmission submission,
                             DeliverableReviewAction action, String comments, User reviewer) {
        this.deliverable = deliverable;
        this.submission = submission;
        this.action = action;
        this.comments = comments;
        this.reviewer = reviewer;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Deliverable getDeliverable() {
        return deliverable;
    }

    public DeliverableSubmission getSubmission() {
        return submission;
    }

    public DeliverableReviewAction getAction() {
        return action;
    }

    public String getComments() {
        return comments;
    }

    public User getReviewer() {
        return reviewer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
