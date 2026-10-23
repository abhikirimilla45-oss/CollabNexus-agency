package com.collabnexus.config;

import com.collabnexus.applications.ApplicationStatus;
import com.collabnexus.applications.CampaignApplication;
import com.collabnexus.applications.CampaignApplicationRepository;
import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.campaigns.Campaign;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.collaborations.Collaboration;
import com.collabnexus.collaborations.CollaborationRepository;
import com.collabnexus.collaborations.CollaborationStatus;
import com.collabnexus.contracts.ContractService;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.deliverables.Deliverable;
import com.collabnexus.deliverables.DeliverableRepository;
import com.collabnexus.deliverables.DeliverableStatus;
import com.collabnexus.messaging.Conversation;
import com.collabnexus.messaging.ConversationMessage;
import com.collabnexus.messaging.ConversationMessageRepository;
import com.collabnexus.messaging.ConversationRepository;
import com.collabnexus.notifications.NotificationService;
import com.collabnexus.notifications.NotificationType;
import com.collabnexus.payments.PaymentRecord;
import com.collabnexus.payments.PaymentRepository;
import com.collabnexus.payments.PaymentStatus;
import com.collabnexus.reviews.Review;
import com.collabnexus.reviews.ReviewRepository;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Adds a small, clearly labelled demonstration workflow so the dashboard can be
 * evaluated without clicking through every screen first. Never created in tests.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "cn.demo-data.enabled", havingValue = "true")
public class DemoWorkspaceSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoWorkspaceSeeder.class);

    private final UserRepository userRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final CollaborationRepository collaborationRepository;
    private final ContractService contractService;
    private final DeliverableRepository deliverableRepository;
    private final PaymentRepository paymentRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;

    public DemoWorkspaceSeeder(UserRepository userRepository,
                               BrandProfileRepository brandProfileRepository,
                               CreatorProfileRepository creatorProfileRepository,
                               CampaignRepository campaignRepository,
                               CampaignApplicationRepository applicationRepository,
                               CollaborationRepository collaborationRepository,
                               ContractService contractService,
                               DeliverableRepository deliverableRepository,
                               PaymentRepository paymentRepository,
                               ConversationRepository conversationRepository,
                               ConversationMessageRepository messageRepository,
                               NotificationService notificationService,
                               ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.campaignRepository = campaignRepository;
        this.applicationRepository = applicationRepository;
        this.collaborationRepository = collaborationRepository;
        this.contractService = contractService;
        this.deliverableRepository = deliverableRepository;
        this.paymentRepository = paymentRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        BrandProfile brand = userRepository.findByEmailIgnoreCase("brand@collabnexus.demo")
                .flatMap(user -> brandProfileRepository.findByUserId(user.getId())).orElse(null);
        CreatorProfile creator = userRepository.findByEmailIgnoreCase("creator@collabnexus.demo")
                .flatMap(user -> creatorProfileRepository.findByUserId(user.getId())).orElse(null);
        if (brand == null || creator == null) {
            return;
        }
        if (applicationRepository.countByCampaignBrandId(brand.getId()) > 0) {
            return;
        }
        List<Campaign> campaigns = campaignRepository.findByBrandId(brand.getId(), PageRequest.of(0, 10)).getContent();
        if (campaigns.size() < 2) {
            return;
        }

        Collaboration active = seedApplication(brand, creator, campaigns.get(0), "3200.00",
                "A demonstration proposal. I will produce a launch reel and a carousel with a clear skincare story.",
                CollaborationStatus.IN_PROGRESS);
        seedDeliverables(active);
        seedPayments(active);
        seedConversation(active);
        contractService.createForCollaboration(active);

        Collaboration completed = seedApplication(brand, creator, campaigns.get(2), "1800.00",
                "A demonstration proposal for the productivity series, clearly labelled as sample data.",
                CollaborationStatus.COMPLETED);
        contractService.createForCollaboration(completed);
        seedCompletedPayment(completed);
        seedReviews(completed);

        notificationService.notify(creator.getUser(), NotificationType.APPLICATION_ACCEPTED,
                "Demo collaboration accepted",
                "This demonstration collaboration is pre-loaded so you can explore the workflow.",
                "/dashboard.html");
        log.info("Demo data: created demonstration applications, collaborations, deliverables, payments, and messages");
    }

    private Collaboration seedApplication(BrandProfile brand, CreatorProfile creator, Campaign campaign,
                                          String price, String proposal, CollaborationStatus status) {
        CampaignApplication application = new CampaignApplication(campaign, creator, proposal);
        application.setProposedPrice(new BigDecimal(price));
        application.setCurrency(campaign.getCurrency());
        application.setAvailableFrom(LocalDate.now().plusDays(7));
        application.setPortfolioUrl("https://example.com/maya-portfolio");
        application.setStatus(ApplicationStatus.ACCEPTED);
        application = applicationRepository.saveAndFlush(application);

        Collaboration collaboration = new Collaboration(application);
        collaboration.setAgreedAmount(application.getProposedPrice());
        collaboration.setCurrency(application.getCurrency());
        collaboration.setStatus(status);
        return collaborationRepository.saveAndFlush(collaboration);
    }

    private void seedDeliverables(Collaboration collaboration) {
        LocalDate today = LocalDate.now();
        Deliverable reel = new Deliverable(collaboration, "Launch reel (demonstration)");
        reel.setDescription("A 30-second launch reel. Demonstration record only.");
        reel.setPlatform("Instagram");
        reel.setContentFormat("Short-form video");
        reel.setDueDate(today.plusDays(10));
        reel.setStatus(DeliverableStatus.IN_PROGRESS);
        deliverableRepository.save(reel);

        Deliverable carousel = new Deliverable(collaboration, "Ingredient carousel (demonstration)");
        carousel.setDescription("Five-slide carousel explaining the fictional product story.");
        carousel.setPlatform("Instagram");
        carousel.setContentFormat("Photo carousel");
        carousel.setDueDate(today.plusDays(21));
        carousel.setStatus(DeliverableStatus.PENDING);
        deliverableRepository.save(carousel);
    }

    private void seedPayments(Collaboration collaboration) {
        PaymentRecord deposit = new PaymentRecord(collaboration, "Deposit (50%)",
                new BigDecimal("1600.00"), collaboration.getCurrency());
        deposit.setDueDate(LocalDate.now().plusDays(5));
        deposit.setStatus(PaymentStatus.DUE);
        deposit.setNotes("Demonstration record. No money was transferred.");
        deposit.setRecordedAt(Instant.now());
        paymentRepository.save(deposit);

        PaymentRecord balance = new PaymentRecord(collaboration, "Balance on approval (50%)",
                new BigDecimal("1600.00"), collaboration.getCurrency());
        balance.setDueDate(LocalDate.now().plusDays(30));
        balance.setStatus(PaymentStatus.NOT_DUE);
        balance.setNotes("Demonstration record. No money was transferred.");
        balance.setRecordedAt(Instant.now());
        paymentRepository.save(balance);
    }

    private void seedCompletedPayment(Collaboration collaboration) {
        PaymentRecord payment = new PaymentRecord(collaboration, "Completed milestone (demonstration)",
                new BigDecimal("1800.00"), collaboration.getCurrency());
        payment.setDueDate(LocalDate.now().minusDays(5));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAmount(new BigDecimal("1800.00"));
        payment.setPaidAt(Instant.now().minusSeconds(86400));
        payment.setExternalReference("DEMO-RECORD-ONLY");
        payment.setNotes("Demonstration record only. No external transfer occurred or is claimed.");
        payment.setRecordedAt(Instant.now().minusSeconds(86400));
        paymentRepository.save(payment);
    }

    private void seedConversation(Collaboration collaboration) {
        Conversation conversation = conversationRepository.saveAndFlush(
                new Conversation(collaboration, collaboration.getCampaign().getTitle()));
        User brandUser = collaboration.getBrand().getUser();
        User creatorUser = collaboration.getCreator().getUser();
        ConversationMessage first = new ConversationMessage(conversation, brandUser,
                "Welcome to the demonstration collaboration. The brief and deadlines are in your workspace.");
        first.setReadAt(Instant.now());
        messageRepository.save(first);
        ConversationMessage second = new ConversationMessage(conversation, creatorUser,
                "Thanks. I have started the launch reel and will submit it before the due date.");
        second.setReadAt(Instant.now());
        messageRepository.save(second);
        conversation.setLastMessageAt(second.getCreatedAt());
        conversationRepository.save(conversation);
    }

    private void seedReviews(Collaboration collaboration) {
        if (reviewRepository.existsByCollaborationIdAndAuthorId(collaboration.getId(),
                collaboration.getBrand().getUser().getId())) {
            return;
        }
        Review brandReview = new Review(collaboration, collaboration.getBrand().getUser(),
                collaboration.getCreator().getUser(), 5,
                "Sample review for the completed demonstration collaboration. Clear communication and on-time delivery.");
        reviewRepository.save(brandReview);
        Review creatorReview = new Review(collaboration, collaboration.getCreator().getUser(),
                collaboration.getBrand().getUser(), 4,
                "Sample review for the completed demonstration collaboration. A clear brief and prompt feedback.");
        reviewRepository.save(creatorReview);
    }
}
