package com.collabnexus.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        String email,

        @NotBlank(message = "is required")
        @Size(max = 72, message = "must be at most 72 characters")
        String password
) {
}
