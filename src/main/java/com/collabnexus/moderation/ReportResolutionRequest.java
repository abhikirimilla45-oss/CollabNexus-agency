package com.collabnexus.moderation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportResolutionRequest(
        @NotNull ReportStatus status,
        @Size(max = 2000) String adminNotes
) {
}
