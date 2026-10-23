package com.collabnexus.brands;

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
@RequestMapping("/api/brand/profile")
public class BrandProfileController {

    private final BrandProfileService brandProfileService;

    public BrandProfileController(BrandProfileService brandProfileService) {
        this.brandProfileService = brandProfileService;
    }

    @GetMapping
    public ApiResponse<BrandView> get(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(brandProfileService.getForUser(user.getId()));
    }

    @PutMapping
    public ApiResponse<BrandView> update(@AuthenticationPrincipal SecurityUser user,
                                         @Valid @RequestBody BrandProfileService.UpdateBrandRequest request) {
        return ApiResponse.ok(brandProfileService.update(user.getId(), request), "Company profile updated.");
    }
}
