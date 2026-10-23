package com.collabnexus.creators;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Public creator profile. Deliberately excludes the account email address so the
 * same representation can be used by directory and public profile pages.
 */
public record CreatorView(
        Long id,
        String displayName,
        String headline,
        String bio,
        String niche,
        String primaryPlatform,
        String country,
        String languages,
        String contentFormats,
        BigDecimal baseRate,
        String currency,
        AvailabilityStatus availability,
        VerificationStatus verificationStatus,
        String portfolioUrl,
        String avatarUrl,
        Long followersCount,
        BigDecimal engagementRate,
        String audienceCountries,
        List<SocialChannelView> socialChannels,
        List<PortfolioItemView> portfolioItems,
        Instant updatedAt
) {

    public static CreatorView from(CreatorProfile profile) {
        return new CreatorView(
                profile.getId(),
                profile.getDisplayName(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getNiche(),
                profile.getPrimaryPlatform(),
                profile.getCountry(),
                profile.getLanguages(),
                profile.getContentFormats(),
                profile.getBaseRate(),
                profile.getCurrency(),
                profile.getAvailability(),
                profile.getVerificationStatus(),
                profile.getPortfolioUrl(),
                profile.getAvatarUrl(),
                profile.getFollowersCount(),
                profile.getEngagementRate(),
                profile.getAudienceCountries(),
                profile.getSocialChannels().stream().map(SocialChannelView::from).toList(),
                profile.getPortfolioItems().stream().map(PortfolioItemView::from).toList(),
                profile.getUpdatedAt()
        );
    }
}
