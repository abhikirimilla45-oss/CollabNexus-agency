package com.collabnexus.messaging;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    public ApiResponse<PageView<ConversationView>> list(@AuthenticationPrincipal SecurityUser user,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(messagingService.list(user.getId(), user.getRole(), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(Map.of("count", messagingService.unreadCount(user.getId(), user.getRole())));
    }

    @PostMapping
    public ApiResponse<ConversationView> create(@AuthenticationPrincipal SecurityUser user,
                                                @Valid @RequestBody ConversationRequest request) {
        return ApiResponse.ok(messagingService.create(user.getId(), user.getRole(), request),
                "Conversation ready.");
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<PageView<MessageView>> messages(@AuthenticationPrincipal SecurityUser user,
                                                       @PathVariable Long id,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(messagingService.messages(user.getId(), user.getRole(), id, page, size));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<MessageView> send(@AuthenticationPrincipal SecurityUser user,
                                         @PathVariable Long id,
                                         @Valid @RequestBody MessageRequest request) {
        return ApiResponse.ok(messagingService.send(user.getId(), user.getRole(), id, request),
                "Message sent.");
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Map<String, Integer>> markRead(@AuthenticationPrincipal SecurityUser user,
                                                      @PathVariable Long id) {
        return ApiResponse.ok(Map.of("updated", messagingService.markRead(user.getId(), user.getRole(), id)));
    }
}
