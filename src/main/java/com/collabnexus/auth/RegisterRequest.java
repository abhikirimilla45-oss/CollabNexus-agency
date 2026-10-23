package com.collabnexus.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "is required")
        @Size(min = 2, max = 120, message = "must be between 2 and 120 characters")
        String fullName,

        @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        @Size(max = 180, message = "must be at most 180 characters")
        String email,

        @NotBlank(message = "is required")
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
        @Pattern(regexp = ".*[a-z].*", message = "must include a lower case letter")
        @Pattern(regexp = ".*[A-Z].*", message = "must include an upper case letter")
        @Pattern(regexp = ".*\\d.*", message = "must include a number")
        String password,

        @NotBlank(message = "is required")
        String role,

        @Size(max = 160, message = "must be at most 160 characters")
        String companyName,

        @Size(max = 255, message = "must be at most 255 characters")
        String companyWebsite,

        @Size(max = 120, message = "must be at most 120 characters")
        String primaryNiche,

        @Size(max = 60, message = "must be at most 60 characters")
        String primaryPlatform,

        @Size(max = 120, message = "must be at most 120 characters")
        String country
) {
}
