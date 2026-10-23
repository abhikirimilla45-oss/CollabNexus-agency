package com.collabnexus.reviews;

import jakarta.validation.constraints.NotNull;

public record ReviewModerationRequest(@NotNull ReviewStatus status) {
}
