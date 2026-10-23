package com.collabnexus.moderation;

import com.collabnexus.common.PageView;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(Long actorId, String action, String entityType, Long entityId, String details) {
        User actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
        String cleaned = StringUtils.hasText(details) && details.length() > 1000
                ? details.substring(0, 1000) : details;
        auditLogRepository.save(new AuditLog(actor, action, entityType, entityId, cleaned));
    }

    @Transactional(readOnly = true)
    public PageView<AuditLogView> list(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return PageView.of(auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(safePage, safeSize)), AuditLogView::from);
    }

    public record AuditLogView(Long id, Long actorId, String actorName, String action,
                               String entityType, Long entityId, String details, Instant createdAt) {
        static AuditLogView from(AuditLog log) {
            return new AuditLogView(log.getId(),
                    log.getActor() == null ? null : log.getActor().getId(),
                    log.getActor() == null ? "System" : log.getActor().getFullName(),
                    log.getAction(), log.getEntityType(), log.getEntityId(), log.getDetails(),
                    log.getCreatedAt());
        }
    }
}
