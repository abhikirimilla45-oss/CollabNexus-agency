package com.collabnexus.moderation;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/api/reports")
    public ApiResponse<ReportView> create(@AuthenticationPrincipal SecurityUser user,
                                          @Valid @RequestBody ReportRequest request) {
        return ApiResponse.ok(reportService.create(user.getId(), request),
                "Report received for moderation review.");
    }

    @GetMapping("/api/admin/reports")
    public ApiResponse<PageView<ReportView>> list(@RequestParam(required = false) ReportStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reportService.list(status, page, size));
    }

    @PostMapping("/api/admin/reports/{id}/resolve")
    public ApiResponse<ReportView> resolve(@AuthenticationPrincipal SecurityUser user,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ReportResolutionRequest request) {
        return ApiResponse.ok(reportService.resolve(user.getId(), id, request), "Report updated.");
    }

    @GetMapping("/api/admin/report-count")
    public ApiResponse<Map<String, Long>> openCount() {
        return ApiResponse.ok(Map.of("count", reportService.list(ReportStatus.OPEN, 0, 1).totalElements()));
    }
}
