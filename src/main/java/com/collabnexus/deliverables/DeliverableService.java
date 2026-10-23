package com.collabnexus.deliverables;

import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.collaborations.CollaborationStatus;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.common.Urls;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.notifications.NotificationService;
import com.collabnexus.notifications.NotificationType;
import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DeliverableService {

    private static final Set<DeliverableStatus> OPEN_STATUSES = EnumSet.of(
            DeliverableStatus.PENDING,
            DeliverableStatus.IN_PROGRESS,
            DeliverableStatus.SUBMITTED,
            DeliverableStatus.REVISION_REQUESTED);

    private final DeliverableRepository deliverableRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final DeliverableReviewRepository reviewRepository;
    private final CollaborationRepository collaborationRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;
    private final DeliverableFileStorage fileStorage;
    private final NotificationService notificationService;

    public DeliverableService(DeliverableRepository deliverableRepository,
                              DeliverableSubmissionRepository submissionRepository,
                              DeliverableReviewRepository reviewRepository,
                              CollaborationRepository collaborationRepository,
                              BrandProfileRepository brandProfileRepository,
                              CreatorProfileRepository creatorProfileRepository,
                              UserRepository userRepository,
                              DeliverableFileStorage fileStorage,
                              NotificationService notificationService) {
        this.deliverableRepository = deliverableRepository;
        this.submissionRepository = submissionRepository;
        this.reviewRepository = reviewRepository;
        this.collaborationRepository = collaborationRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageView<DeliverableView> list(Long userId, Role role, String status, Boolean overdue,
                                          Integer upcomingDays, Long collaborationId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        DeliverableStatus statusFilter = parseStatus(status);
        boolean overdueOnly = Boolean.TRUE.equals(overdue)
                || (StringUtils.hasText(status) && "OVERDUE".equalsIgnoreCase(status.trim()));
        Integer window = upcomingDays == null ? null : Math.min(Math.max(0, upcomingDays), 365);
        LocalDate today = LocalDate.now();

        Specification<Deliverable> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role == Role.BRAND) {
                predicates.add(builder.equal(
                        root.get("collaboration").get("brand").get("user").get("id"), userId));
            } else if (role == Role.CREATOR) {
                predicates.add(builder.equal(
                        root.get("collaboration").get("creator").get("user").get("id"), userId));
            }

            if (collaborationId != null) {
                predicates.add(builder.equal(root.get("collaboration").get("id"), collaborationId));
            }
            if (statusFilter != null) {
                predicates.add(builder.equal(root.get("status"), statusFilter));
            }
            if (overdueOnly) {
                predicates.add(builder.lessThan(root.get("dueDate").as(LocalDate.class), today));
                predicates.add(root.get("status").in(OPEN_STATUSES));
            } else if (window != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("dueDate").as(LocalDate.class), today));
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("dueDate").as(LocalDate.class), today.plusDays(window)));
                predicates.add(root.get("status").in(OPEN_STATUSES));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<Deliverable> results = deliverableRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "dueDate")));
        return PageView.of(results, this::toView);
    }

    @Transactional(readOnly = true)
    public List<DeliverableView> listForCollaboration(Long userId, Role role, Long collaborationId) {
        readableCollaboration(userId, role, collaborationId);
        return deliverableRepository.findByCollaborationIdOrderByDueDateAsc(collaborationId)
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public DeliverableView detail(Long userId, Role role, Long deliverableId) {
        return toView(readable(userId, role, deliverableId));
    }

    @Transactional
    public DeliverableView create(Long userId, Role role, Long collaborationId,
                                  DeliverableRequest request) {
        if (role != Role.BRAND) {
            throw Exceptions.forbidden("Only the brand that owns this collaboration can add deliverables.");
        }
        brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        Collaboration collaboration = collaborationRepository
                .findByIdAndBrandUserId(collaborationId, userId)
                .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        ensureOpen(collaboration);

        Deliverable deliverable = new Deliverable(collaboration, request.title().trim());
        applyRequest(deliverable, request);
        deliverable = deliverableRepository.saveAndFlush(deliverable);
        notificationService.notify(deliverable.getCollaboration().getCreator().getUser(),
                NotificationType.COLLABORATION_UPDATED, "New deliverable added",
                "\"" + deliverable.getTitle() + "\" was added to your collaboration.",
                "/dashboard.html");
        return toView(deliverable);
    }

    @Transactional
    public DeliverableView update(Long userId, Role role, Long deliverableId,
                                  DeliverableRequest request) {
        Deliverable deliverable = brandOwned(userId, role, deliverableId);
        ensureOpen(deliverable.getCollaboration());
        if (deliverable.getStatus() != DeliverableStatus.PENDING) {
            throw Exceptions.conflict("DELIVERABLE_TERMS_LOCKED",
                    "Deliverable terms can only change before the creator starts work.");
        }
        deliverable.setTitle(request.title().trim());
        applyRequest(deliverable, request);
        return toView(deliverableRepository.saveAndFlush(deliverable));
    }

    @Transactional
    public DeliverableView start(Long userId, Long deliverableId) {
        Deliverable deliverable = creatorOwned(userId, deliverableId);
        ensureOpen(deliverable.getCollaboration());
        if (deliverable.getStatus() != DeliverableStatus.PENDING) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a pending deliverable can be started.");
        }
        deliverable.setStatus(DeliverableStatus.IN_PROGRESS);
        return toView(deliverableRepository.saveAndFlush(deliverable));
    }

    @Transactional
    public DeliverableView submit(Long userId, Long deliverableId, String submissionUrl,
                                  String submissionNotes, MultipartFile file) {
        Deliverable deliverable = creatorOwned(userId, deliverableId);
        ensureOpen(deliverable.getCollaboration());
        if (deliverable.getStatus() != DeliverableStatus.IN_PROGRESS
                && deliverable.getStatus() != DeliverableStatus.REVISION_REQUESTED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Start the deliverable and complete any requested revision before submitting.");
        }

        String normalizedUrl = Urls.normalizeHttpUrl(submissionUrl, "Submission URL");
        String notes = trimToNull(submissionNotes);
        if (notes != null && notes.length() > 2000) {
            throw Exceptions.badRequest("INVALID_SUBMISSION",
                    "Submission notes cannot exceed 2000 characters.");
        }
        if (normalizedUrl == null && (file == null || file.isEmpty())) {
            throw Exceptions.badRequest("SUBMISSION_CONTENT_REQUIRED",
                    "Provide a submission URL, a supporting file, or both.");
        }

        DeliverableFileStorage.StoredFile stored = fileStorage.store(file);
        int nextVersion = submissionRepository
                .findFirstByDeliverableIdOrderByVersionNumberDesc(deliverableId)
                .map(existing -> existing.getVersionNumber() + 1)
                .orElse(1);

        User submitter = user(userId);
        DeliverableSubmission submission = new DeliverableSubmission(deliverable, nextVersion, submitter);
        submission.setSubmissionUrl(normalizedUrl);
        submission.setNotes(notes);
        if (stored != null) {
            submission.setOriginalFileName(stored.originalName());
            submission.setStoredFileName(stored.storedName());
            submission.setContentType(stored.contentType());
            submission.setFileSize(stored.size());
        }
        submissionRepository.saveAndFlush(submission);

        deliverable.setStatus(DeliverableStatus.SUBMITTED);
        deliverable.setRevisionNumber(nextVersion);
        deliverable.setSubmittedAt(Instant.now());
        deliverable.setReviewComments(null);
        deliverable.setReviewedAt(null);
        deliverable = deliverableRepository.saveAndFlush(deliverable);
        notificationService.notify(deliverable.getCollaboration().getBrand().getUser(),
                NotificationType.DELIVERABLE_SUBMITTED, "Deliverable submitted",
                "\"" + deliverable.getTitle() + "\" is ready for your review.",
                "/dashboard.html");
        return toView(deliverable);
    }

    @Transactional
    public DeliverableView approve(Long userId, Long deliverableId, String comments) {
        return review(userId, deliverableId, DeliverableReviewAction.APPROVED, comments, false);
    }

    @Transactional
    public DeliverableView requestRevision(Long userId, Long deliverableId, String comments) {
        return review(userId, deliverableId, DeliverableReviewAction.REVISION_REQUESTED, comments, true);
    }

    @Transactional
    public DeliverableView reject(Long userId, Long deliverableId, String comments) {
        return review(userId, deliverableId, DeliverableReviewAction.REJECTED, comments, true);
    }

    @Transactional(readOnly = true)
    public Download download(Long userId, Role role, Long deliverableId, Long submissionId) {
        Deliverable deliverable = readable(userId, role, deliverableId);
        DeliverableSubmission submission = submissionRepository
                .findByDeliverableIdAndId(deliverable.getId(), submissionId)
                .orElseThrow(() -> Exceptions.notFound("Submission not found."));
        if (submission.getStoredFileName() == null) {
            throw Exceptions.notFound("This submission has no supporting file.");
        }
        Resource resource = fileStorage.load(submission.getStoredFileName());
        return new Download(resource, submission.getOriginalFileName(), submission.getContentType());
    }

    private DeliverableView review(Long userId, Long deliverableId, DeliverableReviewAction action,
                                   String comments, boolean commentRequired) {
        Deliverable deliverable = brandOwned(userId, Role.BRAND, deliverableId);
        ensureOpen(deliverable.getCollaboration());
        if (deliverable.getStatus() != DeliverableStatus.SUBMITTED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a submitted deliverable can be reviewed.");
        }
        String cleaned = trimToNull(comments);
        if (commentRequired && cleaned == null) {
            throw Exceptions.badRequest("REVIEW_COMMENT_REQUIRED",
                    "Add review comments so the creator knows what to change.");
        }
        if (cleaned != null && cleaned.length() > 2000) {
            throw Exceptions.badRequest("INVALID_REVIEW",
                    "Review comments cannot exceed 2000 characters.");
        }

        DeliverableSubmission submission = submissionRepository
                .findFirstByDeliverableIdOrderByVersionNumberDesc(deliverableId)
                .orElseThrow(() -> Exceptions.conflict("MISSING_SUBMISSION",
                        "No submission is available to review."));

        User reviewer = user(userId);
        reviewRepository.save(new DeliverableReview(deliverable, submission, action, cleaned, reviewer));

        deliverable.setReviewComments(cleaned);
        deliverable.setReviewedAt(Instant.now());
        deliverable.setStatus(switch (action) {
            case APPROVED -> DeliverableStatus.APPROVED;
            case REVISION_REQUESTED -> DeliverableStatus.REVISION_REQUESTED;
            case REJECTED -> DeliverableStatus.REJECTED;
        });
        deliverable = deliverableRepository.saveAndFlush(deliverable);
        NotificationType type = action == DeliverableReviewAction.APPROVED
                ? NotificationType.DELIVERABLE_APPROVED : NotificationType.REVISION_REQUESTED;
        String message = action == DeliverableReviewAction.APPROVED
                ? "\"" + deliverable.getTitle() + "\" was approved."
                : "Changes were requested for \"" + deliverable.getTitle() + "\".";
        notificationService.notify(deliverable.getCollaboration().getCreator().getUser(),
                type, "Deliverable review", message, "/dashboard.html");
        return toView(deliverable);
    }

    private void applyRequest(Deliverable deliverable, DeliverableRequest request) {
        deliverable.setDescription(trimToNull(request.description()));
        deliverable.setPlatform(trimToNull(request.platform()));
        deliverable.setContentFormat(trimToNull(request.contentFormat()));
        deliverable.setDueDate(request.dueDate());
    }

    private Deliverable readable(Long userId, Role role, Long deliverableId) {
        if (role == Role.BRAND) {
            return deliverableRepository.findByIdAndCollaborationBrandUserId(deliverableId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Deliverable not found."));
        }
        if (role == Role.CREATOR) {
            return deliverableRepository.findByIdAndCollaborationCreatorUserId(deliverableId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Deliverable not found."));
        }
        return deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> Exceptions.notFound("Deliverable not found."));
    }

    private Deliverable brandOwned(Long userId, Role role, Long deliverableId) {
        if (role != Role.BRAND) {
            throw Exceptions.forbidden("Only the brand can review this deliverable.");
        }
        brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        return deliverableRepository.findByIdAndCollaborationBrandUserId(deliverableId, userId)
                .orElseThrow(() -> Exceptions.notFound("Deliverable not found."));
    }

    private Deliverable creatorOwned(Long userId, Long deliverableId) {
        User account = user(userId);
        if (account.getRole() != Role.CREATOR) {
            throw Exceptions.forbidden("Only the assigned creator can act on this deliverable.");
        }
        creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        return deliverableRepository.findByIdAndCollaborationCreatorUserId(deliverableId, userId)
                .orElseThrow(() -> Exceptions.notFound("Deliverable not found."));
    }

    private Collaboration readableCollaboration(Long userId, Role role, Long collaborationId) {
        if (role == Role.BRAND) {
            return collaborationRepository.findByIdAndBrandUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        if (role == Role.CREATOR) {
            return collaborationRepository.findByIdAndCreatorUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        return collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
    }

    private void ensureOpen(Collaboration collaboration) {
        if (collaboration.getStatus() == CollaborationStatus.CANCELLED
                || collaboration.getStatus() == CollaborationStatus.COMPLETED) {
            throw Exceptions.conflict("COLLABORATION_CLOSED",
                    "This collaboration no longer accepts deliverable changes.");
        }
    }

    private User user(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> Exceptions.notFound("Account not found."));
    }

    private DeliverableView toView(Deliverable deliverable) {
        return DeliverableView.from(deliverable,
                submissionRepository.findByDeliverableIdOrderByVersionNumberDesc(deliverable.getId()),
                reviewRepository.findByDeliverableIdOrderByCreatedAtDesc(deliverable.getId()));
    }

    private DeliverableStatus parseStatus(String status) {
        if (!StringUtils.hasText(status) || "OVERDUE".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return DeliverableStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw Exceptions.badRequest("INVALID_STATUS", "Unknown deliverable status.");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record Download(Resource resource, String filename, String contentType) {
    }
}
