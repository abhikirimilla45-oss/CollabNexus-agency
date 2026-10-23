package com.collabnexus.collaborations;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.contracts.Contract;
import com.collabnexus.contracts.ContractRepository;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.deliverables.DeliverableRepository;
import com.collabnexus.deliverables.DeliverableStatus;
import com.collabnexus.notifications.NotificationService;
import com.collabnexus.notifications.NotificationType;
import com.collabnexus.users.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Service
public class CollaborationService {

    private static final Set<DeliverableStatus> OPEN_DELIVERABLE_STATUSES = EnumSet.of(
            DeliverableStatus.PENDING,
            DeliverableStatus.IN_PROGRESS,
            DeliverableStatus.SUBMITTED,
            DeliverableStatus.REVISION_REQUESTED);

    private final CollaborationRepository collaborationRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final ContractRepository contractRepository;
    private final DeliverableRepository deliverableRepository;
    private final NotificationService notificationService;

    public CollaborationService(CollaborationRepository collaborationRepository,
                                BrandProfileRepository brandProfileRepository,
                                CreatorProfileRepository creatorProfileRepository,
                                ContractRepository contractRepository,
                                DeliverableRepository deliverableRepository,
                                NotificationService notificationService) {
        this.collaborationRepository = collaborationRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.contractRepository = contractRepository;
        this.deliverableRepository = deliverableRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageView<CollaborationView> list(Long userId, Role role, CollaborationStatus status,
                                            int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Collaboration> results;
        if (role == Role.BRAND) {
            BrandProfile brand = brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
            results = status == null
                    ? collaborationRepository.findByBrandId(brand.getId(), pageRequest)
                    : collaborationRepository.findByBrandIdAndStatus(brand.getId(), status, pageRequest);
        } else if (role == Role.CREATOR) {
            CreatorProfile creator = creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
            results = status == null
                    ? collaborationRepository.findByCreatorId(creator.getId(), pageRequest)
                    : collaborationRepository.findByCreatorIdAndStatus(creator.getId(), status, pageRequest);
        } else {
            results = status == null
                    ? collaborationRepository.findAll(pageRequest)
                    : collaborationRepository.findByStatus(status, pageRequest);
        }
        return PageView.of(results, this::toView);
    }

    @Transactional(readOnly = true)
    public CollaborationView detail(Long userId, Role role, Long collaborationId) {
        return toView(findForReader(userId, role, collaborationId));
    }

    @Transactional
    public CollaborationView start(Long userId, Role role, Long collaborationId) {
        Collaboration collaboration = participant(userId, role, collaborationId);
        if (collaboration.getStatus() != CollaborationStatus.ACTIVE) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only an active collaboration can be moved into progress.");
        }
        collaboration.setStatus(CollaborationStatus.IN_PROGRESS);
        collaboration = collaborationRepository.saveAndFlush(collaboration);
        notifyCounterpart(collaboration, role, "Collaboration started",
                "Work has started on \"" + collaboration.getCampaign().getTitle() + "\".");
        return toView(collaboration);
    }

    @Transactional
    public CollaborationView complete(Long userId, Role role, Long collaborationId) {
        Collaboration collaboration = participant(userId, role, collaborationId);
        if (collaboration.getStatus() != CollaborationStatus.IN_PROGRESS) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only an in-progress collaboration can be completed.");
        }
        long openDeliverables = deliverableRepository.countByCollaborationIdAndStatusIn(
                collaboration.getId(), OPEN_DELIVERABLE_STATUSES);
        if (openDeliverables > 0) {
            throw Exceptions.conflict("DELIVERABLES_NOT_COMPLETE",
                    "Every deliverable must be approved or rejected before this collaboration can be completed.");
        }
        collaboration.setStatus(CollaborationStatus.COMPLETED);
        collaboration = collaborationRepository.saveAndFlush(collaboration);
        notifyCounterpart(collaboration, role, "Collaboration completed",
                "\"" + collaboration.getCampaign().getTitle()
                        + "\" is complete. Both parties can now submit eligible reviews.");
        return toView(collaboration);
    }

    @Transactional
    public CollaborationView cancel(Long userId, Role role, Long collaborationId) {
        Collaboration collaboration = participant(userId, role, collaborationId);
        if (collaboration.getStatus() != CollaborationStatus.ACTIVE
                && collaboration.getStatus() != CollaborationStatus.IN_PROGRESS) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only an active or in-progress collaboration can be cancelled.");
        }
        collaboration.setStatus(CollaborationStatus.CANCELLED);
        collaboration = collaborationRepository.saveAndFlush(collaboration);
        notifyCounterpart(collaboration, role, "Collaboration cancelled",
                "\"" + collaboration.getCampaign().getTitle() + "\" was cancelled.");
        return toView(collaboration);
    }

    private void notifyCounterpart(Collaboration collaboration, Role role, String title, String message) {
        Long recipientId = role == Role.BRAND
                ? collaboration.getCreator().getUser().getId()
                : collaboration.getBrand().getUser().getId();
        notificationService.notify(recipientId, NotificationType.COLLABORATION_UPDATED,
                title, message, "/dashboard.html");
    }

    private Collaboration participant(Long userId, Role role, Long collaborationId) {
        if (role == Role.BRAND) {
            brandProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
            return collaborationRepository.findByIdAndBrandUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        if (role == Role.CREATOR) {
            creatorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
            return collaborationRepository.findByIdAndCreatorUserId(collaborationId, userId)
                    .orElseThrow(() -> Exceptions.notFound("Collaboration not found."));
        }
        throw Exceptions.forbidden("Administrators can review but not change a collaboration.");
    }

    private CollaborationView toView(Collaboration collaboration) {
        Contract contract = contractRepository.findByCollaborationId(collaboration.getId()).orElse(null);
        long total = deliverableRepository.countByCollaborationId(collaboration.getId());
        long approved = deliverableRepository.countByCollaborationIdAndStatus(
                collaboration.getId(), DeliverableStatus.APPROVED);
        long overdue = deliverableRepository.countByCollaborationIdAndDueDateBeforeAndStatusIn(
                collaboration.getId(), LocalDate.now(), OPEN_DELIVERABLE_STATUSES);
        return CollaborationView.from(collaboration, contract, total, approved, overdue);
    }

    private Collaboration findForReader(Long userId, Role role, Long collaborationId) {
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
}
