package com.collabnexus.creators;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creator/profile")
public class CreatorProfileController {

    private final CreatorProfileService creatorProfileService;

    public CreatorProfileController(CreatorProfileService creatorProfileService) {
        this.creatorProfileService = creatorProfileService;
    }

    @GetMapping
    public ApiResponse<CreatorView> get(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(creatorProfileService.getForUser(user.getId()));
    }

    @PutMapping
    public ApiResponse<CreatorView> update(@AuthenticationPrincipal SecurityUser user,
                                           @Valid @RequestBody CreatorProfileService.UpdateCreatorRequest request) {
        return ApiResponse.ok(creatorProfileService.update(user.getId(), request), "Creator profile updated.");
    }
}
