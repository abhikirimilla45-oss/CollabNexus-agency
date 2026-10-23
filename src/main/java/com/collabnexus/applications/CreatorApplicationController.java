package com.collabnexus.applications;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creator/applications")
@PreAuthorize("hasRole('CREATOR')")
public class CreatorApplicationController {

    private final ApplicationService applicationService;

    public CreatorApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ApiResponse<PageView<ApplicationView>> list(@AuthenticationPrincipal SecurityUser user,
                                                       @RequestParam(required = false) ApplicationStatus status,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(applicationService.listForCreator(user.getId(), status, q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationView> detail(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id) {
        return ApiResponse.ok(applicationService.creatorDetail(user.getId(), id));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<ApplicationView> withdraw(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long id) {
        return ApiResponse.ok(applicationService.withdraw(user.getId(), id), "Application withdrawn.");
    }
}
