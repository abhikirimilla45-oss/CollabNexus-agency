package com.collabnexus.notifications;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageView<NotificationView>> list(@AuthenticationPrincipal SecurityUser user,
                                                        @RequestParam(defaultValue = "false") boolean unread,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(notificationService.list(user.getId(), unread, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount(user.getId())));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationView> markRead(@AuthenticationPrincipal SecurityUser user,
                                                  @PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(user.getId(), id));
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(Map.of("updated", notificationService.markAllRead(user.getId())));
    }
}
