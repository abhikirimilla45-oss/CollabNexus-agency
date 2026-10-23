package com.collabnexus.deliverables;

import jakarta.validation.constraints.Size;

public record DeliverableReviewRequest(
        @Size(max = 2000) String comments
) {
}
