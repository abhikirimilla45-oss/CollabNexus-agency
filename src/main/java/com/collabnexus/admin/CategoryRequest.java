package com.collabnexus.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "is required")
        @Size(max = 80, message = "must be at most 80 characters")
        String name,

        @Size(max = 80, message = "must be at most 80 characters")
        String slug,

        @Size(max = 500, message = "must be at most 500 characters")
        String description,

        @Min(value = 0, message = "must be zero or greater")
        @Max(value = 10000, message = "is too large")
        Integer sortOrder,

        Boolean active
) {
}
