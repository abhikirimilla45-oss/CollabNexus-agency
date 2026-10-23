package com.collabnexus.campaigns;

import com.collabnexus.admin.PlatformSettingsService;
import com.collabnexus.applications.CampaignApplicationRepository;
import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CampaignService {

    private static final Set<String> PUBLIC_SORTS = Set.of("createdAt", "applicationDeadline", "maxBudget", "title");

    private final CampaignRepository campaignRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final PlatformSettingsService settingsService;

    public CampaignService(CampaignRepository campaignRepository,
                           BrandProfileRepository brandProfileRepository,
                           CampaignApplicationRepository applicationRepository,
                           PlatformSettingsService settingsService) {
        this.campaignRepository = campaignRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.applicationRepository = applicationRepository;
        this.settingsService = settingsService;
    }

    @Transactional
    public CampaignView create(Long userId, CampaignRequest request) {
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        Campaign campaign = new Campaign(brand, request.title().trim());
        apply(campaign, request);
        campaign.setStatus(CampaignStatus.DRAFT);
        return CampaignView.from(campaignRepository.saveAndFlush(campaign), 0L);
    }

    @Transactional
    public CampaignView update(Long userId, Long campaignId, CampaignRequest request) {
        Campaign campaign = ownedCampaign(userId, campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only draft campaigns can be edited. Pause the campaign instead of editing live terms.");
        }
        apply(campaign, request);
        return CampaignView.from(campaignRepository.saveAndFlush(campaign),
                applicationRepository.countByCampaignId(campaignId));
    }

    @Transactional
    public CampaignView publish(Long userId, Long campaignId) {
        Campaign campaign = ownedCampaign(userId, campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "Only a draft or paused campaign can be published.");
        }
        if (campaign.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION",
                    "The application deadline has already passed. Update the campaign dates before publishing.");
        }
        campaign.setStatus(CampaignStatus.PUBLISHED);
        return CampaignView.from(campaignRepository.saveAndFlush(campaign),
                applicationRepository.countByCampaignId(campaignId));
    }

    @Transactional
    public CampaignView pause(Long userId, Long campaignId) {
        Campaign campaign = ownedCampaign(userId, campaignId);
        if (campaign.getStatus() != CampaignStatus.PUBLISHED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION", "Only a published campaign can be paused.");
        }
        campaign.setStatus(CampaignStatus.PAUSED);
        return CampaignView.from(campaignRepository.saveAndFlush(campaign),
                applicationRepository.countByCampaignId(campaignId));
    }

    @Transactional
    public CampaignView close(Long userId, Long campaignId) {
        Campaign campaign = ownedCampaign(userId, campaignId);
        if (campaign.getStatus() == CampaignStatus.CLOSED) {
            throw Exceptions.conflict("INVALID_STATE_TRANSITION", "This campaign is already closed.");
        }
        campaign.setStatus(CampaignStatus.CLOSED);
        return CampaignView.from(campaignRepository.saveAndFlush(campaign),
                applicationRepository.countByCampaignId(campaignId));
    }

    @Transactional(readOnly = true)
    public PageView<CampaignView> discover(String q, String niche, String platform, String category,
                                           BigDecimal minBudget, BigDecimal maxBudget,
                                           int page, int size, String sort, String direction) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 48);
        String sortField = PUBLIC_SORTS.contains(sort) ? sort : "createdAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Specification<Campaign> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), CampaignStatus.PUBLISHED));
            predicates.add(builder.greaterThanOrEqualTo(root.get("applicationDeadline"), LocalDate.now()));
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), like),
                        builder.like(builder.lower(root.get("description")), like),
                        builder.like(builder.lower(root.get("brand").get("companyName")), like)
                ));
            }
            if (StringUtils.hasText(niche)) {
                predicates.add(builder.like(builder.lower(root.get("niches")),
                        "%" + niche.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(platform)) {
                predicates.add(builder.like(builder.lower(root.get("platforms")),
                        "%" + platform.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(builder.equal(builder.lower(root.get("category")),
                        category.trim().toLowerCase(Locale.ROOT)));
            }
            if (minBudget != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("maxBudget"), minBudget));
            }
            if (maxBudget != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("minBudget"), maxBudget));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<Campaign> results = campaignRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(sortDirection, sortField)));
        return PageView.of(results, campaign -> CampaignView.from(campaign,
                applicationRepository.countByCampaignId(campaign.getId())));
    }

    @Transactional(readOnly = true)
    public CampaignView publicDetail(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .filter(found -> found.getStatus() == CampaignStatus.PUBLISHED)
                .orElseThrow(() -> Exceptions.notFound("Campaign not found."));
        return CampaignView.from(campaign, applicationRepository.countByCampaignId(campaignId));
    }

    @Transactional(readOnly = true)
    public PageView<CampaignView> listForBrand(Long userId, CampaignStatus status, String q,
                                               int page, int size) {
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        Specification<Campaign> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("brand").get("id"), brand.getId()));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(q)) {
                predicates.add(builder.like(builder.lower(root.get("title")),
                        "%" + q.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<Campaign> results = campaignRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageView.of(results, campaign -> CampaignView.from(campaign,
                applicationRepository.countByCampaignId(campaign.getId())));
    }

    @Transactional(readOnly = true)
    public CampaignView brandDetail(Long userId, Long campaignId) {
        Campaign campaign = ownedCampaign(userId, campaignId);
        return CampaignView.from(campaign, applicationRepository.countByCampaignId(campaignId));
    }

    private Campaign ownedCampaign(Long userId, Long campaignId) {
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        return campaignRepository.findByIdAndBrandId(campaignId, brand.getId())
                .orElseThrow(() -> Exceptions.notFound("Campaign not found."));
    }

    private void apply(Campaign campaign, CampaignRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw Exceptions.badRequest("INVALID_DATES", "The campaign end date cannot be before the start date.");
        }
        if (request.applicationDeadline().isAfter(request.endDate())) {
            throw Exceptions.badRequest("INVALID_DATES", "The application deadline cannot be after the campaign end date.");
        }
        if (request.minBudget().compareTo(request.maxBudget()) > 0) {
            throw Exceptions.badRequest("INVALID_BUDGET", "The minimum budget cannot be greater than the maximum budget.");
        }

        campaign.setTitle(request.title().trim());
        campaign.setDescription(request.description().trim());
        campaign.setObjectives(trimToNull(request.objectives()));
        campaign.setProductDetails(trimToNull(request.productDetails()));
        campaign.setTargetAudience(trimToNull(request.targetAudience()));
        campaign.setPlatforms(trimToNull(request.platforms()));
        campaign.setNiches(trimToNull(request.niches()));
        campaign.setContentFormats(trimToNull(request.contentFormats()));
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        campaign.setApplicationDeadline(request.applicationDeadline());
        campaign.setMinBudget(request.minBudget());
        campaign.setMaxBudget(request.maxBudget());
        campaign.setCurrency(request.currency() == null || request.currency().isBlank()
                ? settingsService.get(PlatformSettingsService.DEFAULT_CURRENCY) : request.currency().trim().toUpperCase(Locale.ROOT));
        campaign.setCompensationModel(request.compensationModel());
        campaign.setCreatorsRequired(request.creatorsRequired());
        campaign.setDeliverableRequirements(trimToNull(request.deliverableRequirements()));
        campaign.setReviewRules(trimToNull(request.reviewRules()));
        campaign.setCategory(trimToNull(request.category()));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
