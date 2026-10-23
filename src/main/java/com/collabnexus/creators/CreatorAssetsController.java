package com.collabnexus.creators;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creator")
@PreAuthorize("hasRole('CREATOR')")
public class CreatorAssetsController {

    private final CreatorAssetsService assetsService;

    public CreatorAssetsController(CreatorAssetsService assetsService) {
        this.assetsService = assetsService;
    }

    @GetMapping("/social-channels")
    public ApiResponse<List<SocialChannelView>> listSocialChannels(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(assetsService.listSocialChannels(user.getId()));
    }

    @PostMapping("/social-channels")
    public ApiResponse<SocialChannelView> addSocialChannel(@AuthenticationPrincipal SecurityUser user,
                                                           @Valid @RequestBody SocialChannelRequest request) {
        return ApiResponse.ok(assetsService.addSocialChannel(user.getId(), request), "Social channel added.");
    }

    @PutMapping("/social-channels/{id}")
    public ApiResponse<SocialChannelView> updateSocialChannel(@AuthenticationPrincipal SecurityUser user,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody SocialChannelRequest request) {
        return ApiResponse.ok(assetsService.updateSocialChannel(user.getId(), id, request),
                "Social channel updated.");
    }

    @DeleteMapping("/social-channels/{id}")
    public ApiResponse<Void> removeSocialChannel(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long id) {
        assetsService.removeSocialChannel(user.getId(), id);
        return ApiResponse.ok(null, "Social channel removed.");
    }

    @GetMapping("/portfolio-items")
    public ApiResponse<List<PortfolioItemView>> listPortfolioItems(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(assetsService.listPortfolioItems(user.getId()));
    }

    @PostMapping("/portfolio-items")
    public ApiResponse<PortfolioItemView> addPortfolioItem(@AuthenticationPrincipal SecurityUser user,
                                                           @Valid @RequestBody PortfolioItemRequest request) {
        return ApiResponse.ok(assetsService.addPortfolioItem(user.getId(), request), "Portfolio item added.");
    }

    @PutMapping("/portfolio-items/{id}")
    public ApiResponse<PortfolioItemView> updatePortfolioItem(@AuthenticationPrincipal SecurityUser user,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody PortfolioItemRequest request) {
        return ApiResponse.ok(assetsService.updatePortfolioItem(user.getId(), id, request),
                "Portfolio item updated.");
    }

    @DeleteMapping("/portfolio-items/{id}")
    public ApiResponse<Void> removePortfolioItem(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long id) {
        assetsService.removePortfolioItem(user.getId(), id);
        return ApiResponse.ok(null, "Portfolio item removed.");
    }
}
