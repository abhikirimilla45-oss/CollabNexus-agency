package com.collabnexus.payments;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentSummary(
        BigDecimal scheduledAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        long recordCount,
        long paidCount,
        long outstandingCount,
        String currency,
        Map<String, Long> byStatus
) {
}
