package com.collabnexus.admin;

import com.collabnexus.campaigns.CampaignStatus;
import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/campaigns")
public class AdminCampaignController {

    private final AdminCampaignService adminCampaignService;

    public AdminCampaignController(AdminCampaignService adminCampaignService) {
        this.adminCampaignService = adminCampaignService;
    }

    @GetMapping
    public ApiResponse<PageView<AdminCampaignService.AdminCampaignView>> list(
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminCampaignService.list(status, q, page, size));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<AdminCampaignService.AdminCampaignView> close(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable Long id) {
        return ApiResponse.ok(adminCampaignService.close(admin.getId(), id),
                "Campaign closed by platform administration.");
    }
}
