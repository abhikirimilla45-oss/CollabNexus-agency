package com.collabnexus.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentView(
        Long id,
        Long collaborationId,
        String campaignTitle,
        String brandName,
        String creatorName,
        String milestone,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String currency,
        LocalDate dueDate,
        PaymentStatus status,
        String externalReference,
        String notes,
        Instant recordedAt,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {

    static PaymentView from(PaymentRecord payment) {
        BigDecimal paid = payment.getPaidAmount() == null ? BigDecimal.ZERO : payment.getPaidAmount();
        BigDecimal remaining = payment.getAmount().subtract(paid).max(BigDecimal.ZERO);
        return new PaymentView(
                payment.getId(),
                payment.getCollaboration().getId(),
                payment.getCollaboration().getCampaign().getTitle(),
                payment.getBrand().getCompanyName(),
                payment.getCreator().getDisplayName(),
                payment.getMilestone(),
                payment.getAmount(),
                paid,
                remaining,
                payment.getCurrency(),
                payment.getDueDate(),
                payment.getStatus(),
                payment.getExternalReference(),
                payment.getNotes(),
                payment.getRecordedAt(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
