package com.collabnexus.creators;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortfolioItemRequest(
        @NotBlank(message = "is required")
        @Size(max = 160, message = "must be at most 160 characters")
        String title,
        @NotBlank(message = "is required")
        @Size(max = 500, message = "must be at most 500 characters")
        String url,
        @Size(max = 1000, message = "must be at most 1000 characters")
        String description
) {
}
