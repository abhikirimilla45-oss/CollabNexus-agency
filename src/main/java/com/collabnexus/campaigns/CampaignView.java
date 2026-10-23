package com.collabnexus.campaigns;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CampaignView(
        Long id,
        String title,
        String description,
        String objectives,
        String productDetails,
        String targetAudience,
        String platforms,
        String niches,
        String contentFormats,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate applicationDeadline,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        String currency,
        CompensationModel compensationModel,
        int creatorsRequired,
        String deliverableRequirements,
        String reviewRules,
        String category,
        CampaignStatus status,
        Long brandId,
        String brandName,
        Long applicationCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static CampaignView from(Campaign campaign) {
        return from(campaign, null);
    }

    public static CampaignView from(Campaign campaign, Long applicationCount) {
        return new CampaignView(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getObjectives(),
                campaign.getProductDetails(),
                campaign.getTargetAudience(),
                campaign.getPlatforms(),
                campaign.getNiches(),
                campaign.getContentFormats(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getApplicationDeadline(),
                campaign.getMinBudget(),
                campaign.getMaxBudget(),
                campaign.getCurrency(),
                campaign.getCompensationModel(),
                campaign.getCreatorsRequired(),
                campaign.getDeliverableRequirements(),
                campaign.getReviewRules(),
                campaign.getCategory(),
                campaign.getStatus(),
                campaign.getBrand().getId(),
                campaign.getBrand().getCompanyName(),
                applicationCount,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
