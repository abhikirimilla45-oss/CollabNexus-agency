package com.collabnexus.moderation;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository,
                         AuditLogService auditLogService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReportView create(Long userId, ReportRequest request) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));
        Report report = new Report(reporter, request.targetType(), request.targetId(),
                request.reason().trim(), trimToNull(request.details()));
        report = reportRepository.saveAndFlush(report);
        return ReportView.from(report);
    }

    @Transactional(readOnly = true)
    public PageView<ReportView> list(ReportStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var results = status == null
                ? reportRepository.findAll(pageRequest)
                : reportRepository.findByStatus(status, pageRequest);
        return PageView.of(results, ReportView::from);
    }

    @Transactional
    public ReportView resolve(Long adminId, Long reportId, ReportResolutionRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> Exceptions.notFound("Report not found."));
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw Exceptions.conflict("REPORT_CLOSED", "This report is already closed.");
        }
        Set<ReportStatus> allowed = Set.of(ReportStatus.UNDER_REVIEW, ReportStatus.RESOLVED,
                ReportStatus.DISMISSED);
        if (!allowed.contains(request.status())) {
            throw Exceptions.badRequest("INVALID_REPORT_STATUS",
                    "Choose under review, resolved, or dismissed.");
        }
        if ((request.status() == ReportStatus.RESOLVED || request.status() == ReportStatus.DISMISSED)
                && (request.adminNotes() == null || request.adminNotes().isBlank())) {
            throw Exceptions.badRequest("RESOLUTION_NOTES_REQUIRED",
                    "Record what action was taken before closing a report.");
        }
        report.setStatus(request.status());
        report.setAdminNotes(trimToNull(request.adminNotes()));
        if (request.status() == ReportStatus.RESOLVED || request.status() == ReportStatus.DISMISSED) {
            report.setResolvedBy(userRepository.findById(adminId).orElse(null));
            report.setResolvedAt(Instant.now());
        }
        report = reportRepository.saveAndFlush(report);
        auditLogService.record(adminId, "REPORT_" + request.status().name(), "Report", report.getId(),
                report.getReason());
        return ReportView.from(report);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
