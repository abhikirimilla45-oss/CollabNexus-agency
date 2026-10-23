package com.collabnexus.support;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @NotNull ContactRole role,
        @NotNull ContactTopic topic,
        @NotBlank @Size(min = 20, max = 4000) String message,
        @NotNull @AssertTrue(message = "Please confirm that you agree to the privacy policy.") Boolean consent
) {
}
