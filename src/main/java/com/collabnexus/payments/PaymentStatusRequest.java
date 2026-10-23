package com.collabnexus.payments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentStatusRequest(
        @NotNull PaymentStatus status,
        BigDecimal paidAmount,
        @Size(max = 160) String externalReference,
        @Size(max = 2000) String notes
) {
}
