package com.collabnexus.analytics;

import java.math.BigDecimal;
import java.util.Map;

public record AdminAnalyticsView(
        long totalUsers,
        long brands,
        long creators,
        long totalCampaigns,
        long publishedCampaigns,
        long totalApplications,
        long totalCollaborations,
        long completedCollaborations,
        long openReports,
        long reviewsUnderReview,
        long paymentRecords,
        BigDecimal scheduledPayments,
        BigDecimal paidPayments,
        Map<String, Long> campaignStatus,
        Map<String, Long> collaborationStatus
) {
}
