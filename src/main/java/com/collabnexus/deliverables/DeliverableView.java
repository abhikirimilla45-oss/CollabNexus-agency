package com.collabnexus.deliverables;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DeliverableView(
        Long id,
        Long collaborationId,
        Long campaignId,
        String campaignTitle,
        String brandName,
        String creatorName,
        String title,
        String description,
        String platform,
        String contentFormat,
        LocalDate dueDate,
        String status,
        String workflowStatus,
        boolean overdue,
        int revisionNumber,
        String reviewComments,
        Instant submittedAt,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt,
        List<DeliverableSubmissionView> submissions,
        List<DeliverableReviewView> reviews
) {

    public static DeliverableView from(Deliverable deliverable,
                                       List<DeliverableSubmission> submissions,
                                       List<DeliverableReview> reviews) {
        boolean overdue = deliverable.getDueDate() != null
                && deliverable.getDueDate().isBefore(LocalDate.now())
                && deliverable.getStatus() != DeliverableStatus.APPROVED
                && deliverable.getStatus() != DeliverableStatus.REJECTED;
        return new DeliverableView(
                deliverable.getId(),
                deliverable.getCollaboration().getId(),
                deliverable.getCollaboration().getCampaign().getId(),
                deliverable.getCollaboration().getCampaign().getTitle(),
                deliverable.getCollaboration().getBrand().getCompanyName(),
                deliverable.getCollaboration().getCreator().getDisplayName(),
                deliverable.getTitle(),
                deliverable.getDescription(),
                deliverable.getPlatform(),
                deliverable.getContentFormat(),
                deliverable.getDueDate(),
                overdue ? "OVERDUE" : deliverable.getStatus().name(),
                deliverable.getStatus().name(),
                overdue,
                deliverable.getRevisionNumber(),
                deliverable.getReviewComments(),
                deliverable.getSubmittedAt(),
                deliverable.getReviewedAt(),
                deliverable.getCreatedAt(),
                deliverable.getUpdatedAt(),
                submissions.stream().map(DeliverableSubmissionView::from).toList(),
                reviews.stream().map(DeliverableReviewView::from).toList()
        );
    }
}
