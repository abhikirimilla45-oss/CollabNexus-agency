package com.collabnexus.admin;

import com.collabnexus.campaigns.Campaign;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.campaigns.CampaignStatus;
import com.collabnexus.common.Exceptions;
import com.collabnexus.common.PageView;
import com.collabnexus.moderation.AuditLogService;
import com.collabnexus.notifications.NotificationService;
import com.collabnexus.notifications.NotificationType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminCampaignService {

    private final CampaignRepository campaignRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AdminCampaignService(CampaignRepository campaignRepository,
                                AuditLogService auditLogService,
                                NotificationService notificationService) {
        this.campaignRepository = campaignRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageView<AdminCampaignView> list(CampaignStatus status, String query, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Specification<Campaign> specification = specification(status, query);
        Page<Campaign> campaigns = campaignRepository.findAll(specification,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageView.of(campaigns, AdminCampaignView::from);
    }

    @Transactional
    public AdminCampaignView close(Long adminId, Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> Exceptions.notFound("Campaign not found."));
        if (campaign.getStatus() == CampaignStatus.CLOSED) {
            return AdminCampaignView.from(campaign);
        }
        campaign.setStatus(CampaignStatus.CLOSED);
        campaign = campaignRepository.saveAndFlush(campaign);
        auditLogService.record(adminId, "CAMPAIGN_CLOSED", "Campaign", campaign.getId(),
                "Campaign \"" + campaign.getTitle() + "\" was closed by platform administration.");
        notificationService.notify(campaign.getBrand().getUser(), NotificationType.COLLABORATION_UPDATED,
                "Campaign closed by administration",
                "The campaign \"" + campaign.getTitle() + "\" was closed after an administrative review.",
                "/dashboard.html");
        return AdminCampaignView.from(campaign);
    }

    private Specification<Campaign> specification(CampaignStatus status, String query) {
        String term = StringUtils.hasText(query)
                ? "%" + query.trim().toLowerCase(Locale.ROOT) + "%" : null;
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (term != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(
                                root.get("brand").get("companyName")), term)));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public record AdminCampaignView(Long id, String title, Long brandId, String brandName,
                                    CampaignStatus status, String category,
                                    BigDecimal minBudget, BigDecimal maxBudget, String currency,
                                    LocalDate applicationDeadline, Instant createdAt) {
        static AdminCampaignView from(Campaign campaign) {
            return new AdminCampaignView(
                    campaign.getId(),
                    campaign.getTitle(),
                    campaign.getBrand().getId(),
                    campaign.getBrand().getCompanyName(),
                    campaign.getStatus(),
                    campaign.getCategory(),
                    campaign.getMinBudget(),
                    campaign.getMaxBudget(),
                    campaign.getCurrency(),
                    campaign.getApplicationDeadline(),
                    campaign.getCreatedAt());
        }
    }
}
