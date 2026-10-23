package com.collabnexus.admin;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final CategoryService categoryService;
    private final PlatformSettingsService settingsService;

    public AdminCatalogController(CategoryService categoryService,
                                  PlatformSettingsService settingsService) {
        this.categoryService = categoryService;
        this.settingsService = settingsService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryService.CategoryView>> categories() {
        return ApiResponse.ok(categoryService.listAll());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryService.CategoryView> createCategory(
            @AuthenticationPrincipal SecurityUser admin,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request, admin.getId()), "Category created.");
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<CategoryService.CategoryView> updateCategory(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.update(id, request, admin.getId()), "Category updated.");
    }

    @GetMapping("/settings")
    public ApiResponse<List<PlatformSettingsService.SettingView>> settings() {
        return ApiResponse.ok(settingsService.list());
    }

    @PutMapping("/settings/{key}")
    public ApiResponse<PlatformSettingsService.SettingView> updateSetting(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable String key,
            @RequestBody SettingUpdateRequest request) {
        return ApiResponse.ok(settingsService.update(key, request.value(), admin.getId()),
                "Setting updated.");
    }

    public record SettingUpdateRequest(String value) {
    }
}
