package com.collabnexus.deliverables;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brand")
public class BrandDeliverableController {

    private final DeliverableService deliverableService;

    public BrandDeliverableController(DeliverableService deliverableService) {
        this.deliverableService = deliverableService;
    }

    @PostMapping("/collaborations/{collaborationId}/deliverables")
    public ApiResponse<DeliverableView> create(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long collaborationId,
                                               @Valid @RequestBody DeliverableRequest request) {
        return ApiResponse.ok(deliverableService.create(user.getId(), user.getRole(),
                collaborationId, request), "Deliverable added to the collaboration.");
    }

    @PutMapping("/deliverables/{id}")
    public ApiResponse<DeliverableView> update(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id,
                                               @Valid @RequestBody DeliverableRequest request) {
        return ApiResponse.ok(deliverableService.update(user.getId(), user.getRole(), id, request),
                "Deliverable terms updated.");
    }

    @PostMapping("/deliverables/{id}/approve")
    public ApiResponse<DeliverableView> approve(@AuthenticationPrincipal SecurityUser user,
                                                @PathVariable Long id,
                                                @RequestBody(required = false)
                                                DeliverableReviewRequest request) {
        return ApiResponse.ok(deliverableService.approve(user.getId(), id,
                request == null ? null : request.comments()), "Deliverable approved.");
    }

    @PostMapping("/deliverables/{id}/request-revision")
    public ApiResponse<DeliverableView> requestRevision(@AuthenticationPrincipal SecurityUser user,
                                                        @PathVariable Long id,
                                                        @RequestBody(required = false)
                                                        DeliverableReviewRequest request) {
        return ApiResponse.ok(deliverableService.requestRevision(user.getId(), id,
                request == null ? null : request.comments()), "Revision requested.");
    }

    @PostMapping("/deliverables/{id}/reject")
    public ApiResponse<DeliverableView> reject(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id,
                                               @RequestBody(required = false)
                                               DeliverableReviewRequest request) {
        return ApiResponse.ok(deliverableService.reject(user.getId(), id,
                request == null ? null : request.comments()), "Deliverable rejected.");
    }
}
