package com.collabnexus.collaborations;

import com.collabnexus.contracts.Contract;
import com.collabnexus.contracts.ContractStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CollaborationView(
        Long id,
        Long applicationId,
        Long campaignId,
        String campaignTitle,
        String campaignStatus,
        LocalDate campaignStartDate,
        LocalDate campaignEndDate,
        Long brandId,
        String brandName,
        Long creatorId,
        String creatorName,
        String creatorNiche,
        String creatorPrimaryPlatform,
        BigDecimal agreedAmount,
        String currency,
        CollaborationStatus status,
        Long contractId,
        String contractReference,
        ContractStatus contractStatus,
        long deliverableCount,
        long approvedDeliverableCount,
        long overdueDeliverableCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static CollaborationView from(Collaboration collaboration) {
        return from(collaboration, null, 0, 0, 0);
    }

    public static CollaborationView from(Collaboration collaboration, Contract contract,
                                         long deliverableCount, long approvedDeliverableCount,
                                         long overdueDeliverableCount) {
        return new CollaborationView(
                collaboration.getId(),
                collaboration.getApplication().getId(),
                collaboration.getCampaign().getId(),
                collaboration.getCampaign().getTitle(),
                collaboration.getCampaign().getStatus().name(),
                collaboration.getCampaign().getStartDate(),
                collaboration.getCampaign().getEndDate(),
                collaboration.getBrand().getId(),
                collaboration.getBrand().getCompanyName(),
                collaboration.getCreator().getId(),
                collaboration.getCreator().getDisplayName(),
                collaboration.getCreator().getNiche(),
                collaboration.getCreator().getPrimaryPlatform(),
                collaboration.getAgreedAmount(),
                collaboration.getCurrency(),
                collaboration.getStatus(),
                contract == null ? null : contract.getId(),
                contract == null ? null : contract.getReferenceNumber(),
                contract == null ? null : contract.getStatus(),
                deliverableCount,
                approvedDeliverableCount,
                overdueDeliverableCount,
                collaboration.getCreatedAt(),
                collaboration.getUpdatedAt()
        );
    }
}
