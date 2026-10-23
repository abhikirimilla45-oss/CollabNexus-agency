package com.collabnexus.applications;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brand/applications")
@PreAuthorize("hasRole('BRAND')")
public class BrandApplicationController {

    private final ApplicationService applicationService;

    public BrandApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ApiResponse<PageView<ApplicationView>> list(@AuthenticationPrincipal SecurityUser user,
                                                       @RequestParam(required = false) ApplicationStatus status,
                                                       @RequestParam(required = false) Long campaignId,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(applicationService.listForBrand(user.getId(), status, campaignId, q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationView> detail(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id) {
        return ApiResponse.ok(applicationService.brandDetail(user.getId(), id));
    }

    @PostMapping("/{id}/shortlist")
    public ApiResponse<ApplicationView> shortlist(@AuthenticationPrincipal SecurityUser user,
                                                  @PathVariable Long id) {
        return ApiResponse.ok(applicationService.shortlist(user.getId(), id), "Application shortlisted.");
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<ApplicationView> accept(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id) {
        return ApiResponse.ok(applicationService.accept(user.getId(), id),
                "Application accepted and collaboration created.");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ApplicationView> reject(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id) {
        return ApiResponse.ok(applicationService.reject(user.getId(), id), "Application rejected.");
    }
}
