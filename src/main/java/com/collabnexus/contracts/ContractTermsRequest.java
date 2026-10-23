package com.collabnexus.contracts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractTermsRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal agreedCompensation,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = 4000) String deliverablesSummary,
        LocalDate completionDeadline,
        @Size(max = 4000) String usageRights,
        @NotNull @Min(0) @Max(20) Integer revisionAllowance,
        @Size(max = 4000) String cancellationTerms,
        @Size(max = 4000) String paymentSchedule
) {
}
