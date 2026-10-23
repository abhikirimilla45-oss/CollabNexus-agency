package com.collabnexus.brands;

import java.time.Instant;

public record BrandView(
        Long id,
        String companyName,
        String website,
        String industry,
        String country,
        String description,
        String logoUrl,
        Instant updatedAt
) {

    public static BrandView from(BrandProfile profile) {
        return new BrandView(
                profile.getId(),
                profile.getCompanyName(),
                profile.getWebsite(),
                profile.getIndustry(),
                profile.getCountry(),
                profile.getDescription(),
                profile.getLogoUrl(),
                profile.getUpdatedAt()
        );
    }
}
