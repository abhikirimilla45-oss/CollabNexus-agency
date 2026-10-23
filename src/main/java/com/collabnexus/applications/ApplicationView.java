package com.collabnexus.applications;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single application as seen by the brand reviewing it or the creator who
 * submitted it. It contains only marketplace data and never account emails or
 * password material.
 */
public record ApplicationView(
        Long id,
        Long campaignId,
        String campaignTitle,
        String campaignStatus,
        LocalDate campaignApplicationDeadline,
        BigDecimal campaignMinBudget,
        BigDecimal campaignMaxBudget,
        String campaignCurrency,
        int campaignCreatorsRequired,
        Long brandId,
        String brandName,
        Long creatorId,
        String creatorName,
        String creatorHeadline,
        String creatorNiche,
        String creatorPrimaryPlatform,
        String creatorCountry,
        String creatorAvailability,
        String creatorVerificationStatus,
        BigDecimal creatorBaseRate,
        String creatorCurrency,
        String proposal,
        BigDecimal proposedPrice,
        String currency,
        LocalDate availableFrom,
        String portfolioUrl,
        ApplicationStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ApplicationView from(CampaignApplication application) {
        return new ApplicationView(
                application.getId(),
                application.getCampaign().getId(),
                application.getCampaign().getTitle(),
                application.getCampaign().getStatus().name(),
                application.getCampaign().getApplicationDeadline(),
                application.getCampaign().getMinBudget(),
                application.getCampaign().getMaxBudget(),
                application.getCampaign().getCurrency(),
                application.getCampaign().getCreatorsRequired(),
                application.getCampaign().getBrand().getId(),
                application.getCampaign().getBrand().getCompanyName(),
                application.getCreator().getId(),
                application.getCreator().getDisplayName(),
                application.getCreator().getHeadline(),
                application.getCreator().getNiche(),
                application.getCreator().getPrimaryPlatform(),
                application.getCreator().getCountry(),
                application.getCreator().getAvailability().name(),
                application.getCreator().getVerificationStatus().name(),
                application.getCreator().getBaseRate(),
                application.getCreator().getCurrency(),
                application.getProposal(),
                application.getProposedPrice(),
                application.getCurrency(),
                application.getAvailableFrom(),
                application.getPortfolioUrl(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
