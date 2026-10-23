package com.collabnexus.moderation;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import com.collabnexus.users.AccountStatus;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminModerationController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AdminModerationController(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageView<AuditLogService.AuditLogView>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(auditLogService.list(page, size));
    }

    @PostMapping("/users/{id}/suspend")
    public ApiResponse<Map<String, Object>> suspend(@AuthenticationPrincipal SecurityUser admin,
                                                    @PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));
        if (user.getRole() == com.collabnexus.users.Role.ADMIN) {
            throw Exceptions.forbidden("Administrator accounts cannot be suspended from this console.");
        }
        if (user.getId().equals(admin.getId())) {
            throw Exceptions.forbidden("You cannot suspend your own account.");
        }
        user.setStatus(AccountStatus.SUSPENDED);
        userRepository.saveAndFlush(user);
        auditLogService.record(admin.getId(), "USER_SUSPENDED", "User", user.getId(),
                "Account " + user.getEmail() + " suspended.");
        return ApiResponse.ok(Map.of("id", user.getId(), "status", user.getStatus().name()),
                "Account suspended.");
    }

    @PostMapping("/users/{id}/activate")
    public ApiResponse<Map<String, Object>> activate(@AuthenticationPrincipal SecurityUser admin,
                                                     @PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.saveAndFlush(user);
        auditLogService.record(admin.getId(), "USER_ACTIVATED", "User", user.getId(),
                "Account " + user.getEmail() + " reactivated.");
        return ApiResponse.ok(Map.of("id", user.getId(), "status", user.getStatus().name()),
                "Account reactivated.");
    }
}
