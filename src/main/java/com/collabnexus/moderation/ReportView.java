package com.collabnexus.moderation;

import java.time.Instant;

public record ReportView(
        Long id,
        Long reporterId,
        String reporterName,
        String reporterEmail,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        String details,
        ReportStatus status,
        String adminNotes,
        String resolvedByName,
        Instant resolvedAt,
        Instant createdAt
) {

    static ReportView from(Report report) {
        return new ReportView(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getFullName(),
                report.getReporter().getEmail(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getAdminNotes(),
                report.getResolvedBy() == null ? null : report.getResolvedBy().getFullName(),
                report.getResolvedAt(),
                report.getCreatedAt()
        );
    }
}
