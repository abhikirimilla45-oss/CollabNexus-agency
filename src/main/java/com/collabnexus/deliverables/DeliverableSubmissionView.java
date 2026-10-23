package com.collabnexus.deliverables;

import java.time.Instant;

public record DeliverableSubmissionView(
        Long id,
        int versionNumber,
        String submissionUrl,
        String notes,
        String originalFileName,
        Long fileSize,
        String contentType,
        boolean hasFile,
        String submittedByName,
        Instant createdAt
) {

    public static DeliverableSubmissionView from(DeliverableSubmission submission) {
        boolean hasFile = submission.getStoredFileName() != null;
        return new DeliverableSubmissionView(
                submission.getId(),
                submission.getVersionNumber(),
                submission.getSubmissionUrl(),
                submission.getNotes(),
                submission.getOriginalFileName(),
                submission.getFileSize(),
                submission.getContentType(),
                hasFile,
                submission.getSubmittedBy().getFullName(),
                submission.getCreatedAt()
        );
    }
}
