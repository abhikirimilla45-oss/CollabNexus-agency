package com.collabnexus.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ContractVersionView(
        Long id,
        int versionNumber,
        BigDecimal agreedCompensation,
        String currency,
        String deliverablesSummary,
        LocalDate completionDeadline,
        String usageRights,
        int revisionAllowance,
        String cancellationTerms,
        String paymentSchedule,
        String createdByName,
        Instant createdAt
) {

    public static ContractVersionView from(ContractVersion version) {
        return new ContractVersionView(
                version.getId(),
                version.getVersionNumber(),
                version.getAgreedCompensation(),
                version.getCurrency(),
                version.getDeliverablesSummary(),
                version.getCompletionDeadline(),
                version.getUsageRights(),
                version.getRevisionAllowance(),
                version.getCancellationTerms(),
                version.getPaymentSchedule(),
                version.getCreatedBy().getFullName(),
                version.getCreatedAt()
        );
    }
}
