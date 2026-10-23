package com.collabnexus.analytics;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/brand/analytics/overview")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BrandAnalyticsView> brand(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(analyticsService.brand(user.getId()));
    }

    @GetMapping("/api/creator/analytics/overview")
    @PreAuthorize("hasRole('CREATOR')")
    public ApiResponse<CreatorAnalyticsView> creator(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(analyticsService.creator(user.getId()));
    }

    @GetMapping("/api/admin/analytics/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminAnalyticsView> admin() {
        return ApiResponse.ok(analyticsService.admin());
    }
}
