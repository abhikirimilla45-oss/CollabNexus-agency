package com.collabnexus.applications;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignApplicationController {

    private final ApplicationService applicationService;

    public CampaignApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/{campaignId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CREATOR')")
    public ApiResponse<ApplicationView> apply(@AuthenticationPrincipal SecurityUser user,
                                              @PathVariable Long campaignId,
                                              @Valid @RequestBody ApplicationRequest request) {
        return ApiResponse.ok(applicationService.apply(user.getId(), campaignId, request),
                "Application submitted.");
    }
}
