package com.collabnexus.payments;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotBlank @Size(max = 120) String milestone,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        LocalDate dueDate,
        @Size(max = 2000) String notes
) {
}
