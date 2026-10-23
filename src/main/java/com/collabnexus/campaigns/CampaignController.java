package com.collabnexus.campaigns;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public ApiResponse<PageView<CampaignView>> discover(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String niche,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.ok(campaignService.discover(q, niche, platform, category,
                minBudget, maxBudget, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignView> detail(@PathVariable Long id) {
        return ApiResponse.ok(campaignService.publicDetail(id));
    }
}
