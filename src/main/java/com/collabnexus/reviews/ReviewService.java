package com.collabnexus.reviews;

import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.collaborations.CollaborationStatus;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.moderation.AuditLogService;
import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CollaborationRepository collaborationRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ReviewService(ReviewRepository reviewRepository,
                         CollaborationRepository collaborationRepository,
                         CreatorProfileRepository creatorProfileRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService) {
        this.reviewRepository = reviewRepository;
        this.collaborationRepository = collaborationRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReviewView create(Long userId, Long collaborationId, ReviewRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));
        Collaboration collaboration;
        User subject;
        if (author.getRole() == Role.BRAND) {
            collaboration = collaborationRepository.findByIdAndBrandUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
            subject = collaboration.getCreator().getUser();
        } else if (author.getRole() == Role.CREATOR) {
            collaboration = collaborationRepository.findByIdAndCreatorUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
            subject = collaboration.getBrand().getUser();
        } else {
            throw Exceptions.forbidden("Administrators cannot submit reviews.");
        }

        if (collaboration.getStatus() != CollaborationStatus.COMPLETED) {
            throw Exceptions.conflict("COLLABORATION_NOT_COMPLETED",
                    "Reviews are available only after a collaboration is completed.");
        }
        if (reviewRepository.existsByCollaborationIdAndAuthorId(collaborationId, userId)) {
            throw Exceptions.conflict("DUPLICATE_REVIEW",
                    "You have already reviewed this collaboration.");
        }

        Review review = new Review(collaboration, author, subject, request.rating(),
                request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
        review = reviewRepository.saveAndFlush(review);
        return ReviewView.from(review);
    }

    @Transactional(readOnly = true)
    public PageView<ReviewView> mine(Long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        return PageView.of(reviewRepository.findByAuthorId(userId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))),
                ReviewView::from);
    }

    @Transactional(readOnly = true)
    public PageView<ReviewView> received(Long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        return PageView.of(reviewRepository.findBySubjectIdAndStatus(userId, ReviewStatus.PUBLISHED,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))),
                ReviewView::from);
    }

    @Transactional(readOnly = true)
    public ReviewSummary receivedSummary(Long userId) {
        long count = reviewRepository.countBySubjectIdAndStatus(userId, ReviewStatus.PUBLISHED);
        if (count == 0) {
            return new ReviewSummary(BigDecimal.ZERO, 0, Map.of());
        }
        var reviews = reviewRepository.findBySubjectIdAndStatus(userId, ReviewStatus.PUBLISHED,
                PageRequest.of(0, 500)).getContent();
        int total = reviews.stream().mapToInt(Review::getRating).sum();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            int value = star;
            distribution.put(String.valueOf(star), reviews.stream()
                    .filter(review -> review.getRating() == value).count());
        }
        return new ReviewSummary(BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP), count, distribution);
    }

    @Transactional(readOnly = true)
    public PageView<ReviewView> forCreator(Long creatorId, int page, int size) {
        CreatorProfile creator = creatorProfileRepository.findById(creatorId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        return PageView.of(reviewRepository.findBySubjectIdAndStatus(creator.getUser().getId(),
                ReviewStatus.PUBLISHED,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))),
                ReviewView::from);
    }

    @Transactional(readOnly = true)
    public ReviewSummary summaryForCreator(Long creatorId) {
        CreatorProfile creator = creatorProfileRepository.findById(creatorId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        Long subjectId = creator.getUser().getId();
        long count = reviewRepository.countBySubjectIdAndStatus(subjectId, ReviewStatus.PUBLISHED);
        if (count == 0) {
            return new ReviewSummary(BigDecimal.ZERO, 0, Map.of());
        }
        var reviews = reviewRepository.findBySubjectIdAndStatus(subjectId, ReviewStatus.PUBLISHED,
                PageRequest.of(0, 500)).getContent();
        int total = reviews.stream().mapToInt(Review::getRating).sum();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            int value = star;
            distribution.put(String.valueOf(star), reviews.stream()
                    .filter(review -> review.getRating() == value).count());
        }
        return new ReviewSummary(BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP), count, distribution);
    }

    @Transactional(readOnly = true)
    public PageView<ReviewView> adminList(ReviewStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var results = status == null
                ? reviewRepository.findAll(pageRequest)
                : reviewRepository.findByStatus(status, pageRequest);
        return PageView.of(results, ReviewView::from);
    }

    @Transactional
    public ReviewView moderate(Long adminId, Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> Exceptions.notFound("Review not found."));
        review.setStatus(status);
        review = reviewRepository.saveAndFlush(review);
        auditLogService.record(adminId, "REVIEW_" + status.name(), "Review", review.getId(),
                "Review status set to " + status.name());
        return ReviewView.from(review);
    }

    public record ReviewSummary(BigDecimal averageRating, long reviewCount, Map<String, Long> distribution) {
    }
}
