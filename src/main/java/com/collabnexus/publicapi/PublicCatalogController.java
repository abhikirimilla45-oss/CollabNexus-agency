package com.collabnexus.publicapi;

import com.collabnexus.admin.CategoryService;
import com.collabnexus.admin.PlatformSettingsService;
import com.collabnexus.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {

    private final CategoryService categoryService;
    private final PlatformSettingsService settingsService;

    public PublicCatalogController(CategoryService categoryService,
                                   PlatformSettingsService settingsService) {
        this.categoryService = categoryService;
        this.settingsService = settingsService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryService.CategoryView>> categories() {
        return ApiResponse.ok(categoryService.listActive());
    }

    @GetMapping("/settings")
    public ApiResponse<Map<String, String>> settings() {
        return ApiResponse.ok(settingsService.publicSettings());
    }
}
