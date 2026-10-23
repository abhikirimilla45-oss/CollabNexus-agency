package com.collabnexus.reviews;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/collaborations/{collaborationId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewView> create(@AuthenticationPrincipal SecurityUser user,
                                          @PathVariable Long collaborationId,
                                          @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.create(user.getId(), collaborationId, request),
                "Review published.");
    }

    @GetMapping("/api/reviews/mine")
    public ApiResponse<PageView<ReviewView>> mine(@AuthenticationPrincipal SecurityUser user,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.mine(user.getId(), page, size));
    }

    @GetMapping("/api/reviews/received")
    public ApiResponse<PageView<ReviewView>> received(@AuthenticationPrincipal SecurityUser user,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.received(user.getId(), page, size));
    }

    @GetMapping("/api/reviews/received/summary")
    public ApiResponse<ReviewService.ReviewSummary> receivedSummary(
            @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(reviewService.receivedSummary(user.getId()));
    }

    @GetMapping("/api/reviews/creator/{creatorId}")
    public ApiResponse<PageView<ReviewView>> forCreator(@PathVariable Long creatorId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.forCreator(creatorId, page, size));
    }

    @GetMapping("/api/reviews/creator/{creatorId}/summary")
    public ApiResponse<ReviewService.ReviewSummary> summary(@PathVariable Long creatorId) {
        return ApiResponse.ok(reviewService.summaryForCreator(creatorId));
    }

    @GetMapping("/api/admin/reviews")
    public ApiResponse<PageView<ReviewView>> adminList(@RequestParam(required = false) ReviewStatus status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.adminList(status, page, size));
    }

    @PostMapping("/api/admin/reviews/{id}/status")
    public ApiResponse<ReviewView> moderate(@AuthenticationPrincipal SecurityUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ReviewModerationRequest request) {
        return ApiResponse.ok(reviewService.moderate(user.getId(), id, request.status()),
                "Review status updated.");
    }
}
