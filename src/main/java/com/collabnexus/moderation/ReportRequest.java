package com.collabnexus.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull ReportTargetType targetType,
        Long targetId,
        @NotBlank @Size(max = 120) String reason,
        @Size(max = 2000) String details
) {
}
