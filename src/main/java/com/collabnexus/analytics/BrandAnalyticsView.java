package com.collabnexus.analytics;

import java.math.BigDecimal;
import java.util.Map;

public record BrandAnalyticsView(
        long totalCampaigns,
        long activeCampaigns,
        long totalApplications,
        long shortlistedApplications,
        long acceptedApplications,
        long totalCollaborations,
        long completedCollaborations,
        BigDecimal plannedBudget,
        BigDecimal recordedSpending,
        long deliverablesAwaitingReview,
        double deliverableCompletionRate,
        Map<String, Long> applicationFunnel,
        Map<String, Long> monthlyApplications,
        Map<String, Long> monthlyCollaborations,
        String currency
) {
}
