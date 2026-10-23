package com.collabnexus.campaigns;

import com.collabnexus.applications.ApplicationService;
import com.collabnexus.applications.ApplicationStatus;
import com.collabnexus.applications.ApplicationView;
import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brand/campaigns")
public class BrandCampaignController {

    private final CampaignService campaignService;
    private final ApplicationService applicationService;

    public BrandCampaignController(CampaignService campaignService,
                                   ApplicationService applicationService) {
        this.campaignService = campaignService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public ApiResponse<PageView<CampaignView>> list(@AuthenticationPrincipal SecurityUser user,
                                                    @RequestParam(required = false) CampaignStatus status,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(campaignService.listForBrand(user.getId(), status, q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignView> detail(@AuthenticationPrincipal SecurityUser user,
                                            @PathVariable Long id) {
        return ApiResponse.ok(campaignService.brandDetail(user.getId(), id));
    }

    @GetMapping("/{id}/applications")
    public ApiResponse<PageView<ApplicationView>> applications(@AuthenticationPrincipal SecurityUser user,
                                                               @PathVariable Long id,
                                                               @RequestParam(required = false) ApplicationStatus status,
                                                               @RequestParam(required = false) String q,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(applicationService.listForBrand(user.getId(), status, id, q, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CampaignView> create(@AuthenticationPrincipal SecurityUser user,
                                            @Valid @RequestBody CampaignRequest request) {
        return ApiResponse.ok(campaignService.create(user.getId(), request), "Campaign draft created.");
    }

    @PutMapping("/{id}")
    public ApiResponse<CampaignView> update(@AuthenticationPrincipal SecurityUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody CampaignRequest request) {
        return ApiResponse.ok(campaignService.update(user.getId(), id, request), "Campaign draft updated.");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<CampaignView> publish(@AuthenticationPrincipal SecurityUser user,
                                             @PathVariable Long id) {
        return ApiResponse.ok(campaignService.publish(user.getId(), id), "Campaign published.");
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<CampaignView> pause(@AuthenticationPrincipal SecurityUser user,
                                           @PathVariable Long id) {
        return ApiResponse.ok(campaignService.pause(user.getId(), id), "Campaign paused.");
    }

    @PostMapping("/{id}/close")
    public ApiResponse<CampaignView> close(@AuthenticationPrincipal SecurityUser user,
                                           @PathVariable Long id) {
        return ApiResponse.ok(campaignService.close(user.getId(), id), "Campaign closed.");
    }
}
