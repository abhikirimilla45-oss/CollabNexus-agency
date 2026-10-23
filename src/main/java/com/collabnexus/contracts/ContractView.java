package com.collabnexus.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContractView(
        Long id,
        String referenceNumber,
        Long collaborationId,
        Long campaignId,
        String campaignTitle,
        String brandName,
        String creatorName,
        BigDecimal agreedCompensation,
        String currency,
        String deliverablesSummary,
        LocalDate completionDeadline,
        String usageRights,
        int revisionAllowance,
        String cancellationTerms,
        String paymentSchedule,
        ContractStatus status,
        int versionNumber,
        boolean brandAcknowledged,
        boolean creatorAcknowledged,
        Instant brandAcknowledgedAt,
        Instant creatorAcknowledgedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ContractVersionView> versions,
        List<ContractAcceptanceView> acceptances
) {

    public static ContractView from(Contract contract, List<ContractVersion> versions,
                                    List<ContractAcceptance> acceptances) {
        return new ContractView(
                contract.getId(),
                contract.getReferenceNumber(),
                contract.getCollaboration().getId(),
                contract.getCollaboration().getCampaign().getId(),
                contract.getCollaboration().getCampaign().getTitle(),
                contract.getBrand().getCompanyName(),
                contract.getCreator().getDisplayName(),
                contract.getAgreedCompensation(),
                contract.getCurrency(),
                contract.getDeliverablesSummary(),
                contract.getCompletionDeadline(),
                contract.getUsageRights(),
                contract.getRevisionAllowance(),
                contract.getCancellationTerms(),
                contract.getPaymentSchedule(),
                contract.getStatus(),
                contract.getVersionNumber(),
                contract.getBrandAcknowledgedAt() != null,
                contract.getCreatorAcknowledgedAt() != null,
                contract.getBrandAcknowledgedAt(),
                contract.getCreatorAcknowledgedAt(),
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                versions.stream().map(ContractVersionView::from).toList(),
                acceptances.stream().map(ContractAcceptanceView::from).toList()
        );
    }
}
