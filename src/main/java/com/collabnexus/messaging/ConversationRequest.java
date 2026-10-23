package com.collabnexus.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConversationRequest(
        @NotNull Long collaborationId,
        @Size(max = 160) String subject
) {
}
