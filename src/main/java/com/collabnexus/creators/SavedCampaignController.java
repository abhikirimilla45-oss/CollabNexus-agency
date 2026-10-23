package com.collabnexus.creators;

import com.collabnexus.campaigns.CampaignView;
import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creator/saved-campaigns")
@PreAuthorize("hasRole('CREATOR')")
public class SavedCampaignController {

    private final SavedCampaignService savedCampaignService;

    public SavedCampaignController(SavedCampaignService savedCampaignService) {
        this.savedCampaignService = savedCampaignService;
    }

    @GetMapping
    public ApiResponse<PageView<CampaignView>> list(@AuthenticationPrincipal SecurityUser user,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(savedCampaignService.list(user.getId(), page, size));
    }

    @GetMapping("/ids")
    public ApiResponse<List<Long>> ids(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(savedCampaignService.ids(user.getId()));
    }

    @PostMapping("/{campaignId}")
    public ApiResponse<CampaignView> save(@AuthenticationPrincipal SecurityUser user,
                                          @PathVariable Long campaignId) {
        return ApiResponse.ok(savedCampaignService.save(user.getId(), campaignId), "Campaign saved.");
    }

    @DeleteMapping("/{campaignId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal SecurityUser user,
                                    @PathVariable Long campaignId) {
        savedCampaignService.remove(user.getId(), campaignId);
        return ApiResponse.ok(null, "Campaign removed from saved campaigns.");
    }
}
