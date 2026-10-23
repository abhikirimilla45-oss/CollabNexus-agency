package com.collabnexus.analytics;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignMetricRequest(
        @NotNull(message = "is required") LocalDate recordedOn,
        @Min(value = 0, message = "must be zero or greater") Long reach,
        @Min(value = 0, message = "must be zero or greater") Long impressions,
        @Min(value = 0, message = "must be zero or greater") Long clicks,
        @Min(value = 0, message = "must be zero or greater") Long conversions,
        @DecimalMin(value = "0.00", message = "must be zero or greater")
        @Digits(integer = 10, fraction = 2, message = "supports up to 2 decimal places") BigDecimal spend,
        @DecimalMin(value = "0.00", message = "must be zero or greater")
        @Digits(integer = 10, fraction = 2, message = "supports up to 2 decimal places") BigDecimal revenue,
        @Size(max = 120, message = "must be at most 120 characters") String source,
        @Size(max = 2000, message = "must be at most 2000 characters") String notes
) {

    @AssertTrue(message = "Record at least one reach, impression, click, conversion, spend, or revenue value.")
    public boolean hasAtLeastOneResult() {
        return reach != null || impressions != null || clicks != null || conversions != null
                || spend != null || revenue != null;
    }
}
