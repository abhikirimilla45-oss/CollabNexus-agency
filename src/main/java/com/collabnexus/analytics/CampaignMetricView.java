package com.collabnexus.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CampaignMetricView(
        Long id,
        Long campaignId,
        String campaignTitle,
        LocalDate recordedOn,
        Long reach,
        Long impressions,
        Long clicks,
        Long conversions,
        BigDecimal spend,
        BigDecimal revenue,
        String source,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static CampaignMetricView from(CampaignMetric metric) {
        return new CampaignMetricView(
                metric.getId(),
                metric.getCampaign().getId(),
                metric.getCampaign().getTitle(),
                metric.getRecordedOn(),
                metric.getReach(),
                metric.getImpressions(),
                metric.getClicks(),
                metric.getConversions(),
                metric.getSpend(),
                metric.getRevenue(),
                metric.getSource(),
                metric.getNotes(),
                metric.getCreatedAt(),
                metric.getUpdatedAt());
    }
}
