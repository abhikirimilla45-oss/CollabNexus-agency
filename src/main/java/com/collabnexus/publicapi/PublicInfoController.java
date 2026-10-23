package com.collabnexus.publicapi;

import com.collabnexus.admin.PlatformSettingsService;
import com.collabnexus.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicInfoController {

    private final String applicationName;
    private final String version;
    private final String phase;
    private final PlatformSettingsService settingsService;

    public PublicInfoController(@Value("${info.app.name:CollabNexus}") String applicationName,
                                @Value("${info.app.version:unknown}") String version,
                                @Value("${info.app.phase:unknown}") String phase,
                                PlatformSettingsService settingsService) {
        this.applicationName = applicationName;
        this.version = version;
        this.phase = phase;
        this.settingsService = settingsService;
    }

    @GetMapping("/info")
    public ApiResponse<AppInfo> info() {
        return ApiResponse.ok(new AppInfo(
                applicationName,
                "Where Brand Meets Influence",
                version,
                phase,
                settingsService.get(PlatformSettingsService.MARKETPLACE_STATUS),
                List.of("Java 21", "Spring Boot 3.5", "Spring Data JPA", "Spring Security", "H2 (file-based)")
        ));
    }

    @GetMapping("/ping")
    public ApiResponse<Ping> ping() {
        return ApiResponse.ok(new Ping("pong", Instant.now()));
    }

    public record AppInfo(
            String name,
            String tagline,
            String version,
            String phase,
            String marketplaceStatus,
            List<String> stack
    ) {
    }

    public record Ping(String message, Instant timestamp) {
    }
}
