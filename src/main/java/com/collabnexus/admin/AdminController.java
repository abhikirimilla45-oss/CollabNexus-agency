package com.collabnexus.admin;

import com.collabnexus.applications.CampaignApplicationRepository;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.campaigns.CampaignStatus;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.moderation.ReportRepository;
import com.collabnexus.moderation.ReportStatus;
import com.collabnexus.payments.PaymentRepository;
import com.collabnexus.reviews.ReviewRepository;
import com.collabnexus.reviews.ReviewStatus;
import com.collabnexus.users.AccountStatus;
import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final CollaborationRepository collaborationRepository;
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;

    public AdminController(UserRepository userRepository,
                           CampaignRepository campaignRepository,
                           CampaignApplicationRepository applicationRepository,
                           CollaborationRepository collaborationRepository,
                           ReportRepository reportRepository,
                           ReviewRepository reviewRepository,
                           PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.applicationRepository = applicationRepository;
        this.collaborationRepository = collaborationRepository;
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverview> overview() {
        Map<String, Long> roles = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            roles.put(role.name(), userRepository.countByRole(role));
        }
        long total = userRepository.count();
        long suspended = userRepository.countByStatus(AccountStatus.SUSPENDED);
        return ApiResponse.ok(new AdminOverview(
                total,
                roles.get("BRAND"),
                roles.get("CREATOR"),
                roles.get("ADMIN"),
                suspended,
                campaignRepository.count(),
                campaignRepository.countByStatus(CampaignStatus.PUBLISHED),
                applicationRepository.count(),
                collaborationRepository.count(),
                reportRepository.countByStatus(ReportStatus.OPEN),
                reviewRepository.countByStatus(ReviewStatus.UNDER_REVIEW),
                paymentRepository.count()
        ));
    }

    @GetMapping("/users")
    public ApiResponse<PageView<AdminUserView>> users(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = role == null
                ? userRepository.findAll(pageRequest)
                : userRepository.findByRole(role, pageRequest);
        return ApiResponse.ok(PageView.of(users, AdminUserView::from));
    }

    public record AdminOverview(long totalUsers, long brands, long creators, long administrators,
                                long suspended, long totalCampaigns, long activeCampaigns,
                                long totalApplications, long totalCollaborations, long openReports,
                                long reviewsUnderReview, long paymentRecords) {
    }

    public record AdminUserView(Long id, String email, String fullName, String role, String status,
                                Instant createdAt) {
        public static AdminUserView from(User user) {
            return new AdminUserView(user.getId(), user.getEmail(), user.getFullName(),
                    user.getRole().name(), user.getStatus().name(), user.getCreatedAt());
        }
    }
}
