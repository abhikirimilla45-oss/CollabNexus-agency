package com.collabnexus.analytics;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.campaigns.Campaign;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.Exceptions;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/brand")
public class CampaignMetricController {

    private final CampaignMetricRepository metricRepository;
    private final CampaignRepository campaignRepository;
    private final BrandProfileRepository brandProfileRepository;

    public CampaignMetricController(CampaignMetricRepository metricRepository,
                                    CampaignRepository campaignRepository,
                                    BrandProfileRepository brandProfileRepository) {
        this.metricRepository = metricRepository;
        this.campaignRepository = campaignRepository;
        this.brandProfileRepository = brandProfileRepository;
    }

    @GetMapping("/campaigns/{campaignId}/metrics")
    @Transactional(readOnly = true)
    public ApiResponse<List<CampaignMetricView>> list(@AuthenticationPrincipal SecurityUser user,
                                                      @PathVariable Long campaignId) {
        ownedCampaign(user.getId(), campaignId);
        return ApiResponse.ok(metricRepository.findByCampaignIdOrderByRecordedOnDescIdDesc(campaignId)
                .stream().map(CampaignMetricView::from).toList());
    }

    @PostMapping("/campaigns/{campaignId}/metrics")
    @Transactional
    public ApiResponse<CampaignMetricView> create(@AuthenticationPrincipal SecurityUser user,
                                                  @PathVariable Long campaignId,
                                                  @Valid @RequestBody CampaignMetricRequest request) {
        Campaign campaign = ownedCampaign(user.getId(), campaignId);
        CampaignMetric metric = new CampaignMetric(campaign, request.recordedOn());
        metric.setReach(request.reach());
        metric.setImpressions(request.impressions());
        metric.setClicks(request.clicks());
        metric.setConversions(request.conversions());
        metric.setSpend(request.spend());
        metric.setRevenue(request.revenue());
        metric.setSource(trim(request.source()));
        metric.setNotes(trim(request.notes()));
        metricRepository.saveAndFlush(metric);
        return ApiResponse.ok(CampaignMetricView.from(metric),
                "Manual campaign result recorded. These figures are self-reported, not verified platform data.");
    }

    @DeleteMapping("/metrics/{metricId}")
    @Transactional
    public ApiResponse<Void> delete(@AuthenticationPrincipal SecurityUser user,
                                    @PathVariable Long metricId) {
        CampaignMetric metric = metricRepository.findByIdAndCampaignBrandUserId(metricId, user.getId())
                .orElseThrow(() -> Exceptions.notFound("Campaign result not found."));
        metricRepository.delete(metric);
        return ApiResponse.ok(null, "Manual campaign result removed.");
    }

    @GetMapping("/analytics/manual-results")
    @Transactional(readOnly = true)
    public ApiResponse<ManualResultsSummary> summary(@AuthenticationPrincipal SecurityUser user) {
        BrandProfile brand = brandProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        Long brandId = brand.getId();
        return ApiResponse.ok(new ManualResultsSummary(
                metricRepository.sumReachByBrandId(brandId),
                metricRepository.sumImpressionsByBrandId(brandId),
                metricRepository.sumClicksByBrandId(brandId),
                metricRepository.sumConversionsByBrandId(brandId),
                metricRepository.sumSpendByBrandId(brandId),
                metricRepository.sumRevenueByBrandId(brandId)));
    }

    private Campaign ownedCampaign(Long userId, Long campaignId) {
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        return campaignRepository.findByIdAndBrandId(campaignId, brand.getId())
                .orElseThrow(() -> Exceptions.notFound("Campaign not found."));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record ManualResultsSummary(Long reach, Long impressions, Long clicks, Long conversions,
                                       BigDecimal spend, BigDecimal revenue) {
    }
}
