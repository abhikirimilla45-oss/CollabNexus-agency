package com.collabnexus.applications;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.campaigns.Campaign;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.campaigns.CampaignStatus;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.common.Urls;
import com.collabnexus.contracts.ContractService;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.messaging.MessagingService;
import com.collabnexus.notifications.NotificationService;
import com.collabnexus.notifications.NotificationType;
import com.collabnexus.users.Role;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ApplicationService {

    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CollaborationRepository collaborationRepository;
    private final ContractService contractService;
    private final NotificationService notificationService;
    private final MessagingService messagingService;

    public ApplicationService(CampaignApplicationRepository applicationRepository,
                              CampaignRepository campaignRepository,
                              CreatorProfileRepository creatorProfileRepository,
                              BrandProfileRepository brandProfileRepository,
                              CollaborationRepository collaborationRepository,
                              ContractService contractService,
                              NotificationService notificationService,
                              MessagingService messagingService) {
        this.applicationRepository = applicationRepository;
        this.campaignRepository = campaignRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.collaborationRepository = collaborationRepository;
        this.contractService = contractService;
        this.notificationService = notificationService;
        this.messagingService = messagingService;
    }

    @Transactional
    public ApplicationView apply(Long userId, Long campaignId, ApplicationRequest request) {
        CreatorProfile creator = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> Exceptions.notFound("Campaign not found."));

        if (campaign.getStatus() != CampaignStatus.PUBLISHED) {
            throw Exceptions.conflict("CAMPAIGN_NOT_ACCEPTING_APPLICATIONS",
                    "This campaign is not currently accepting applications.");
        }
        if (campaign.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw Exceptions.conflict("APPLICATION_DEADLINE_PASSED",
                    "The application deadline for this campaign has passed.");
        }
        if (applicationRepository.existsByCampaignIdAndCreatorId(campaignId, creator.getId())) {
            throw Exceptions.conflict("DUPLICATE_APPLICATION",
                    "You have already applied to this campaign.");
        }

        CampaignApplication application = new CampaignApplication(campaign, creator, request.proposal().trim());
        application.setProposedPrice(request.proposedPrice());
        application.setCurrency(normalizeCurrency(request.currency(), campaign.getCurrency()));
        application.setAvailableFrom(request.availableFrom());
        application.setPortfolioUrl(Urls.normalizeHttpUrl(request.portfolioUrl(), "Portfolio URL"));

        try {
            application = applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException ex) {
            throw Exceptions.conflict("DUPLICATE_APPLICATION",
                    "You have already applied to this campaign.");
        }
        notificationService.notify(campaign.getBrand().getUser(), NotificationType.APPLICATION_RECEIVED,
                "New campaign application",
                creator.getDisplayName() + " applied to \"" + campaign.getTitle() + "\".",
                "/dashboard.html");
        return ApplicationView.from(application);
    }

    @Transactional(readOnly = true)
    public PageView<ApplicationView> listForCreator(Long userId, ApplicationStatus status, String q,
                                                    int page, int size) {
        CreatorProfile creator = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        Specification<CampaignApplication> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("creator").get("id"), creator.getId()));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("campaign").get("title")), like),
                        builder.like(builder.lower(root.get("campaign").get("brand").get("companyName")), like),
                        builder.like(builder.lower(root.get("proposal")), like)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<CampaignApplication> results = applicationRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageView.of(results, ApplicationView::from);
    }

    @Transactional(readOnly = true)
    public ApplicationView creatorDetail(Long userId, Long applicationId) {
        return ApplicationView.from(applicationRepository.findByIdAndCreatorUserId(applicationId, userId)
                .orElseThrow(() -> Exceptions.notFound("Application not found.")));
    }

    @Transactional
    public ApplicationView withdraw(Long userId, Long applicationId) {
        CampaignApplication application = applicationRepository.findByIdAndCreatorUserId(applicationId, userId)
                .orElseThrow(() -> Exceptions.notFound("Application not found."));

        if (application.getStatus() != ApplicationStatus.PENDING
                && application.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a pending or shortlisted application can be withdrawn.");
        }
        if (application.getCampaign().getApplicationDeadline().isBefore(LocalDate.now())) {
            throw Exceptions.conflict("WITHDRAWAL_CLOSED",
                    "Applications can no longer be withdrawn after the campaign deadline.");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        return ApplicationView.from(applicationRepository.saveAndFlush(application));
    }

    @Transactional(readOnly = true)
    public PageView<ApplicationView> listForBrand(Long userId, ApplicationStatus status, Long campaignId,
                                                  String q, int page, int size) {
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        Specification<CampaignApplication> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("campaign").get("brand").get("id"), brand.getId()));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (campaignId != null) {
                predicates.add(builder.equal(root.get("campaign").get("id"), campaignId));
            }
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("creator").get("displayName")), like),
                        builder.like(builder.lower(root.get("creator").get("niche")), like),
                        builder.like(builder.lower(root.get("campaign").get("title")), like),
                        builder.like(builder.lower(root.get("proposal")), like)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<CampaignApplication> results = applicationRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageView.of(results, ApplicationView::from);
    }

    @Transactional(readOnly = true)
    public ApplicationView brandDetail(Long userId, Long applicationId) {
        return ApplicationView.from(brandApplication(userId, applicationId));
    }

    @Transactional
    public ApplicationView shortlist(Long userId, Long applicationId) {
        CampaignApplication application = brandApplication(userId, applicationId);
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a pending application can be shortlisted.");
        }
        application.setStatus(ApplicationStatus.SHORTLISTED);
        application = applicationRepository.saveAndFlush(application);
        notificationService.notify(application.getCreator().getUser(),
                NotificationType.APPLICATION_SHORTLISTED, "Application shortlisted",
                "Your application for \"" + application.getCampaign().getTitle() + "\" was shortlisted.",
                "/dashboard.html");
        return ApplicationView.from(application);
    }

    @Transactional
    public ApplicationView reject(Long userId, Long applicationId) {
        CampaignApplication application = brandApplication(userId, applicationId);
        if (application.getStatus() != ApplicationStatus.PENDING
                && application.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a pending or shortlisted application can be rejected.");
        }
        application.setStatus(ApplicationStatus.REJECTED);
        application = applicationRepository.saveAndFlush(application);
        notificationService.notify(application.getCreator().getUser(),
                NotificationType.APPLICATION_REJECTED, "Application not selected",
                "Your application for \"" + application.getCampaign().getTitle() + "\" was not selected.",
                "/dashboard.html");
        return ApplicationView.from(application);
    }

    /**
     * Accepting is the point where money and capacity matter. The campaign row is
     * locked for the duration of the transaction so two brands (or two requests)
     * cannot accept more creators than the campaign allows.
     */
    @Transactional
    public ApplicationView accept(Long userId, Long applicationId) {
        CampaignApplication application = brandApplication(userId, applicationId);
        if (application.getStatus() != ApplicationStatus.PENDING
                && application.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a pending or shortlisted application can be accepted.");
        }

        Long campaignId = application.getCampaign().getId();
        Campaign lockedCampaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> Exceptions.notFound("Campaign not found."));

        if (lockedCampaign.getStatus() == CampaignStatus.DRAFT
                || lockedCampaign.getStatus() == CampaignStatus.CLOSED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Applications cannot be accepted for a draft or closed campaign.");
        }

        long accepted = applicationRepository.countByCampaignIdAndStatus(campaignId,
                ApplicationStatus.ACCEPTED);
        if (accepted >= lockedCampaign.getCreatorsRequired()) {
            throw Exceptions.conflict("CAMPAIGN_CAPACITY_REACHED",
                    "This campaign already has the maximum number of accepted creators.");
        }

        application.setStatus(ApplicationStatus.ACCEPTED);
        application = applicationRepository.saveAndFlush(application);

        Collaboration collaboration = new Collaboration(application);
        collaboration.setAgreedAmount(application.getProposedPrice());
        collaboration.setCurrency(application.getCurrency());
        collaboration = collaborationRepository.saveAndFlush(collaboration);
        contractService.createForCollaboration(collaboration);
        messagingService.ensureForCollaboration(userId, Role.BRAND, collaboration.getId());
        notificationService.notify(application.getCreator().getUser(),
                NotificationType.APPLICATION_ACCEPTED, "Application accepted",
                "You were accepted for \"" + application.getCampaign().getTitle()
                        + "\". Review the contract and deliverables next.",
                "/dashboard.html");

        return ApplicationView.from(application);
    }

    private CampaignApplication brandApplication(Long userId, Long applicationId) {
        brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        return applicationRepository.findByIdAndCampaignBrandUserId(applicationId, userId)
                .orElseThrow(() -> Exceptions.notFound("Application not found."));
    }

    private String normalizeCurrency(String requested, String fallback) {
        String value = StringUtils.hasText(requested) ? requested : fallback;
        return value == null ? "USD" : value.trim().toUpperCase(Locale.ROOT);
    }
}
