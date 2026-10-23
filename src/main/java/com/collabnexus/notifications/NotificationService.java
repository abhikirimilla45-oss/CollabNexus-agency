package com.collabnexus.notifications;

import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notify(User recipient, NotificationType type, String title, String message, String link) {
        if (recipient == null) {
            return;
        }
        notificationRepository.save(new Notification(recipient, type, clean(title, 160),
                clean(message, 600), clean(link, 300)));
    }

    @Transactional
    public void notify(Long recipientId, NotificationType type, String title, String message, String link) {
        if (recipientId == null) {
            return;
        }
        userRepository.findById(recipientId).ifPresent(user ->
                notify(user, type, title, message, link));
    }

    @Transactional(readOnly = true)
    public PageView<NotificationView> list(Long userId, boolean unreadOnly, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        var results = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageRequest)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return PageView.of(results, NotificationView::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationView markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> Exceptions.notFound("Notification not found."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return NotificationView.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, Instant.now());
    }

    private String clean(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
