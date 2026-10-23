package com.collabnexus.applications;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationRequest(
        @NotBlank(message = "is required")
        @Size(max = 2000, message = "must be at most 2000 characters")
        String proposal,

        @DecimalMin(value = "0.00", message = "must be zero or greater")
        @DecimalMax(value = "100000000.00", message = "is too large")
        @Digits(integer = 10, fraction = 2, message = "supports up to 2 decimal places")
        BigDecimal proposedPrice,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three letter currency code")
        String currency,

        LocalDate availableFrom,

        @Size(max = 500, message = "must be at most 500 characters")
        String portfolioUrl
) {
}
