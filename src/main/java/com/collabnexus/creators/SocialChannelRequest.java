package com.collabnexus.creators;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SocialChannelRequest(
        @NotBlank(message = "is required")
        @Size(max = 60, message = "must be at most 60 characters")
        String platform,
        @NotBlank(message = "is required")
        @Size(max = 120, message = "must be at most 120 characters")
        String handle,
        @Size(max = 500, message = "must be at most 500 characters")
        String url,
        @PositiveOrZero(message = "must be zero or greater")
        Long followersCount,
        @DecimalMin(value = "0.00", message = "must be zero or greater")
        @DecimalMax(value = "100.00", message = "must be at most 100")
        @Digits(integer = 3, fraction = 2, message = "supports up to 2 decimal places")
        BigDecimal engagementRate
) {
}
