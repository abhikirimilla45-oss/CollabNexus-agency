package com.collabnexus.campaigns;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignRequest(
        @NotBlank(message = "is required")
        @Size(max = 160, message = "must be at most 160 characters")
        String title,

        @NotBlank(message = "is required")
        @Size(max = 4000, message = "must be at most 4000 characters")
        String description,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String objectives,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String productDetails,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String targetAudience,

        @Size(max = 255, message = "must be at most 255 characters")
        String platforms,

        @Size(max = 255, message = "must be at most 255 characters")
        String niches,

        @Size(max = 255, message = "must be at most 255 characters")
        String contentFormats,

        @NotNull(message = "is required")
        LocalDate startDate,

        @NotNull(message = "is required")
        LocalDate endDate,

        @NotNull(message = "is required")
        LocalDate applicationDeadline,

        @NotNull(message = "is required")
        @DecimalMin(value = "0.00", message = "must be zero or greater")
        @DecimalMax(value = "100000000.00", message = "is too large")
        @Digits(integer = 10, fraction = 2, message = "supports up to 2 decimal places")
        BigDecimal minBudget,

        @NotNull(message = "is required")
        @DecimalMin(value = "0.00", message = "must be zero or greater")
        @DecimalMax(value = "100000000.00", message = "is too large")
        @Digits(integer = 10, fraction = 2, message = "supports up to 2 decimal places")
        BigDecimal maxBudget,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three letter currency code")
        String currency,

        @NotNull(message = "is required")
        CompensationModel compensationModel,

        @NotNull(message = "is required")
        @Min(value = 1, message = "must be at least 1")
        @Max(value = 100, message = "must be at most 100")
        Integer creatorsRequired,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String deliverableRequirements,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String reviewRules,

        @Size(max = 120, message = "must be at most 120 characters")
        String category
) {
}
