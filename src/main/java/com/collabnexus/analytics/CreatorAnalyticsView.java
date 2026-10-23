package com.collabnexus.analytics;

import java.math.BigDecimal;
import java.util.Map;

public record CreatorAnalyticsView(
        long totalApplications,
        long pendingApplications,
        long acceptedApplications,
        long totalCollaborations,
        long activeCollaborations,
        long completedCollaborations,
        long activeDeliverables,
        long upcomingDeadlines,
        BigDecimal recordedEarnings,
        long outstandingPaymentRecords,
        double responseRate,
        Map<String, Long> monthlyApplications,
        Map<String, Long> monthlyEarnings,
        String currency
) {
}
