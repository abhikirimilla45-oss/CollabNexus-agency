package com.collabnexus.analytics;

import com.collabnexus.applications.ApplicationStatus;
import com.collabnexus.applications.CampaignApplication;
import com.collabnexus.applications.CampaignApplicationRepository;
import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.campaigns.CampaignStatus;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.collaborations.CollaborationStatus;
import com.collabnexus.common.Exceptions;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.deliverables.DeliverableRepository;
import com.collabnexus.deliverables.DeliverableStatus;
import com.collabnexus.moderation.ReportRepository;
import com.collabnexus.moderation.ReportStatus;
import com.collabnexus.payments.PaymentRecord;
import com.collabnexus.payments.PaymentRepository;
import com.collabnexus.payments.PaymentStatus;
import com.collabnexus.reviews.ReviewRepository;
import com.collabnexus.reviews.ReviewStatus;
import com.collabnexus.users.Role;
import com.collabnexus.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsService {

    private static final Set<DeliverableStatus> OPEN_DELIVERABLES = EnumSet.of(
            DeliverableStatus.PENDING,
            DeliverableStatus.IN_PROGRESS,
            DeliverableStatus.SUBMITTED,
            DeliverableStatus.REVISION_REQUESTED);

    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final CollaborationRepository collaborationRepository;
    private final DeliverableRepository deliverableRepository;
    private final PaymentRepository paymentRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;

    public AnalyticsService(CampaignRepository campaignRepository,
                            CampaignApplicationRepository applicationRepository,
                            CollaborationRepository collaborationRepository,
                            DeliverableRepository deliverableRepository,
                            PaymentRepository paymentRepository,
                            BrandProfileRepository brandProfileRepository,
                            CreatorProfileRepository creatorProfileRepository,
                            UserRepository userRepository,
                            ReportRepository reportRepository,
                            ReviewRepository reviewRepository) {
        this.campaignRepository = campaignRepository;
        this.applicationRepository = applicationRepository;
        this.collaborationRepository = collaborationRepository;
        this.deliverableRepository = deliverableRepository;
        this.paymentRepository = paymentRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public BrandAnalyticsView brand(Long userId) {
        BrandProfile brand = brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Brand profile not found."));
        Long brandId = brand.getId();
        List<CampaignApplication> applications = applicationRepository.findByCampaignBrandId(brandId);
        List<Collaboration> collaborations = collaborationRepository.findByBrandIdOrderByCreatedAtDesc(brandId);

        Map<String, Long> funnel = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            funnel.put(status.name(), applicationRepository
                    .countByCampaignBrandIdAndStatus(brandId, status));
        }

        long deliverableCount = deliverableRepository.countByCollaborationBrandUserId(userId);
        long approved = deliverableRepository.countByCollaborationBrandUserIdAndStatus(
                userId, DeliverableStatus.APPROVED);

        return new BrandAnalyticsView(
                campaignRepository.countByBrandId(brandId),
                campaignRepository.countByBrandIdAndStatus(brandId, CampaignStatus.PUBLISHED),
                applicationRepository.countByCampaignBrandId(brandId),
                funnel.getOrDefault(ApplicationStatus.SHORTLISTED.name(), 0L),
                funnel.getOrDefault(ApplicationStatus.ACCEPTED.name(), 0L),
                collaborationRepository.countByBrandId(brandId),
                collaborationRepository.countByBrandIdAndStatus(brandId, CollaborationStatus.COMPLETED),
                campaignRepository.sumMaxBudgetByBrandId(brandId),
                paymentRepository.sumPaidByBrandId(brandId),
                deliverableRepository.countByCollaborationBrandUserIdAndStatus(
                        userId, DeliverableStatus.SUBMITTED),
                deliverableCount == 0 ? 0 : round((approved * 100.0) / deliverableCount),
                funnel,
                monthlyCounts(applications.stream().map(CampaignApplication::getCreatedAt).toList()),
                monthlyCounts(collaborations.stream().map(Collaboration::getCreatedAt).toList()),
                "USD"
        );
    }

    @Transactional(readOnly = true)
    public CreatorAnalyticsView creator(Long userId) {
        CreatorProfile creator = creatorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> Exceptions.notFound("Creator profile not found."));
        Long creatorId = creator.getId();
        List<CampaignApplication> applications = applicationRepository.findByCreatorId(creatorId);
        List<Collaboration> collaborations = collaborationRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId);
        LocalDate today = LocalDate.now();
        long activeDeliverables = 0;
        for (DeliverableStatus status : OPEN_DELIVERABLES) {
            activeDeliverables += deliverableRepository
                    .countByCollaborationCreatorUserIdAndStatus(userId, status);
        }

        long accepted = applicationRepository.countByCreatorIdAndStatus(creatorId, ApplicationStatus.ACCEPTED);
        long rejected = applicationRepository.countByCreatorIdAndStatus(creatorId, ApplicationStatus.REJECTED);
        long decided = accepted + rejected;

        List<PaymentRecord> payments = paymentRepository.findByCreatorId(creatorId);
        return new CreatorAnalyticsView(
                applicationRepository.countByCreatorId(creatorId),
                applicationRepository.countByCreatorIdAndStatus(creatorId, ApplicationStatus.PENDING),
                accepted,
                collaborationRepository.countByCreatorId(creatorId),
                collaborationRepository.countByCreatorIdAndStatus(creatorId, CollaborationStatus.ACTIVE)
                        + collaborationRepository.countByCreatorIdAndStatus(creatorId,
                        CollaborationStatus.IN_PROGRESS),
                collaborationRepository.countByCreatorIdAndStatus(creatorId, CollaborationStatus.COMPLETED),
                activeDeliverables,
                deliverableRepository.countByCollaborationCreatorUserIdAndDueDateBetweenAndStatusIn(
                        userId, today, today.plusDays(30), OPEN_DELIVERABLES),
                paymentRepository.sumPaidByCreatorId(creatorId),
                payments.stream().filter(payment -> payment.getStatus() != PaymentStatus.PAID
                        && payment.getStatus() != PaymentStatus.REFUNDED).count(),
                decided == 0 ? 0 : round((accepted * 100.0) / decided),
                monthlyCounts(applications.stream().map(CampaignApplication::getCreatedAt).toList()),
                monthlyPaid(payments),
                "USD"
        );
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsView admin() {
        Map<String, Long> campaignStatus = new LinkedHashMap<>();
        for (CampaignStatus status : CampaignStatus.values()) {
            campaignStatus.put(status.name(), campaignRepository.countByStatus(status));
        }
        Map<String, Long> collaborationStatus = new LinkedHashMap<>();
        collaborationStatus.put("ACTIVE", collaborationRepository.countByStatus(CollaborationStatus.ACTIVE));
        collaborationStatus.put("IN_PROGRESS", collaborationRepository.countByStatus(CollaborationStatus.IN_PROGRESS));
        collaborationStatus.put("COMPLETED", collaborationRepository.countByStatus(CollaborationStatus.COMPLETED));
        collaborationStatus.put("CANCELLED", collaborationRepository.countByStatus(CollaborationStatus.CANCELLED));

        return new AdminAnalyticsView(
                userRepository.count(),
                userRepository.countByRole(Role.BRAND),
                userRepository.countByRole(Role.CREATOR),
                campaignRepository.count(),
                campaignRepository.countByStatus(CampaignStatus.PUBLISHED),
                applicationRepository.count(),
                collaborationRepository.count(),
                collaborationRepository.countByStatus(CollaborationStatus.COMPLETED),
                reportRepository.countByStatus(ReportStatus.OPEN),
                reviewRepository.countByStatus(ReviewStatus.UNDER_REVIEW),
                paymentRepository.count(),
                paymentRepository.sumAllScheduled(),
                paymentRepository.sumAllPaid(),
                campaignStatus,
                collaborationStatus
        );
    }

    private Map<String, Long> monthlyCounts(List<Instant> timestamps) {
        Map<String, Long> months = emptyMonths();
        for (Instant timestamp : timestamps) {
            String key = key(timestamp);
            months.computeIfPresent(key, (ignored, value) -> value + 1);
        }
        return months;
    }

    private Map<String, Long> monthlyPaid(List<PaymentRecord> payments) {
        Map<String, Long> months = emptyMonths();
        for (PaymentRecord payment : payments) {
            Instant at = payment.getPaidAt();
            if (at == null || payment.getPaidAmount() == null || payment.getPaidAmount().signum() == 0) {
                continue;
            }
            String key = key(at);
            months.computeIfPresent(key, (ignored, value) -> value + payment.getPaidAmount().longValue());
        }
        return months;
    }

    private Map<String, Long> emptyMonths() {
        Map<String, Long> months = new LinkedHashMap<>();
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        for (int offset = 5; offset >= 0; offset--) {
            months.put(current.minusMonths(offset).toString(), 0L);
        }
        return months;
    }

    private String key(Instant timestamp) {
        return YearMonth.from(timestamp.atZone(ZoneOffset.UTC)).toString();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
