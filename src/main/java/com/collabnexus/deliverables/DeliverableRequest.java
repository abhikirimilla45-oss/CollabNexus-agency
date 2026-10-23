package com.collabnexus.deliverables;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DeliverableRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 2000) String description,
        @Size(max = 80) String platform,
        @Size(max = 80) String contentFormat,
        @NotNull LocalDate dueDate
) {
}
