package com.collabnexus.config;

import com.collabnexus.brands.BrandProfile;
import com.collabnexus.brands.BrandProfileRepository;
import com.collabnexus.campaigns.Campaign;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.campaigns.CampaignStatus;
import com.collabnexus.campaigns.CompensationModel;
import com.collabnexus.creators.AvailabilityStatus;
import com.collabnexus.creators.CreatorProfile;
import com.collabnexus.creators.CreatorProfileRepository;
import com.collabnexus.creators.VerificationStatus;
import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Creates clearly labelled demonstration accounts on first run. These are only
 * for local evaluation and are never created when the test profile is active.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "cn.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "Demo!Pass1234";

    private final UserRepository userRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final CampaignRepository campaignRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository,
                          BrandProfileRepository brandProfileRepository,
                          CreatorProfileRepository creatorProfileRepository,
                          CampaignRepository campaignRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.creatorProfileRepository = creatorProfileRepository;
        this.campaignRepository = campaignRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createAdmin();
        createBrand();
        createCreator();
        createCampaigns();
    }

    private void createAdmin() {
        if (userRepository.existsByEmailIgnoreCase("admin@collabnexus.demo")) {
            return;
        }
        User admin = new User("admin@collabnexus.demo", passwordEncoder.encode(DEMO_PASSWORD),
                "Platform Administrator", Role.ADMIN);
        userRepository.save(admin);
        log.info("Demo data: created administrator account admin@collabnexus.demo");
    }

    private void createBrand() {
        if (userRepository.existsByEmailIgnoreCase("brand@collabnexus.demo")) {
            return;
        }
        User user = new User("brand@collabnexus.demo", passwordEncoder.encode(DEMO_PASSWORD),
                "Jordan Ellis", Role.BRAND);
        userRepository.save(user);
        BrandProfile profile = new BrandProfile(user, "Northwind Atelier");
        profile.setWebsite("https://example.com/northwind");
        profile.setIndustry("Fashion & Lifestyle");
        profile.setCountry("United Kingdom");
        profile.setDescription("A demonstration brand profile for the CollabNexus portfolio project. "
                + "Northwind Atelier is a fictional premium lifestyle label.");
        brandProfileRepository.save(profile);
        log.info("Demo data: created brand account brand@collabnexus.demo");
    }

    private void createCreator() {
        if (userRepository.existsByEmailIgnoreCase("creator@collabnexus.demo")) {
            return;
        }
        User user = new User("creator@collabnexus.demo", passwordEncoder.encode(DEMO_PASSWORD),
                "Maya Chen", Role.CREATOR);
        userRepository.save(user);
        CreatorProfile profile = new CreatorProfile(user, "Maya Chen");
        profile.setHeadline("Beauty and skincare storyteller");
        profile.setBio("Demonstration creator profile. Maya is a fictional creator used to show how "
                + "CollabNexus presents niches, platforms, availability, and indicative rates. "
                + "No real audience metrics are shown.");
        profile.setNiche("Beauty & Skincare");
        profile.setPrimaryPlatform("Instagram");
        profile.setCountry("Singapore");
        profile.setLanguages("English, Mandarin");
        profile.setContentFormats("Short-form video, Photo carousel");
        profile.setBaseRate(new BigDecimal("1200.00"));
        profile.setCurrency("USD");
        profile.setAvailability(AvailabilityStatus.AVAILABLE);
        profile.setVerificationStatus(VerificationStatus.UNVERIFIED);
        profile.setPortfolioUrl("https://example.com/maya-portfolio");
        creatorProfileRepository.save(profile);
        log.info("Demo data: created creator account creator@collabnexus.demo");
    }

    private void createCampaigns() {
        BrandProfile brand = userRepository.findByEmailIgnoreCase("brand@collabnexus.demo")
                .flatMap(user -> brandProfileRepository.findByUserId(user.getId()))
                .orElse(null);
        if (brand == null || campaignRepository.countByBrandId(brand.getId()) > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        seedCampaign(brand, "Glow Ritual launch",
                "A three-part launch story introducing a new barrier-repair serum to a skincare-conscious audience. "
                        + "Demonstration campaign data for the CollabNexus portfolio project.",
                "Awareness", "Beauty & Skincare", "Instagram, TikTok",
                "Short-form video, Photo carousel", new BigDecimal("2000.00"), new BigDecimal("4000.00"),
                3, today.plusDays(21), today.plusDays(28), today.plusDays(90));
        seedCampaign(brand, "City escape series",
                "Short-form city guides for a boutique hotel collection across three European destinations. "
                        + "Demonstration campaign data, not a live brief.",
                "Consideration", "Travel & Hospitality", "YouTube, Instagram",
                "Long-form video, Short-form video", new BigDecimal("3500.00"), new BigDecimal("6000.00"),
                4, today.plusDays(35), today.plusDays(42), today.plusDays(120));
        seedCampaign(brand, "Focus mode challenge",
                "A seven-day productivity series showing how small teams protect deep work without burnout. "
                        + "Demonstration campaign data for interface evaluation only.",
                "Community", "Technology & Productivity", "TikTok, YouTube",
                "Short-form video, Live stream", new BigDecimal("1500.00"), new BigDecimal("2500.00"),
                2, today.plusDays(14), today.plusDays(21), today.plusDays(75));
        log.info("Demo data: created {} demonstration campaigns", 3);
    }

    private void seedCampaign(BrandProfile brand, String title, String description, String category,
                              String niches, String platforms, String contentFormats,
                              BigDecimal minBudget, BigDecimal maxBudget, int creatorsRequired,
                              LocalDate deadline, LocalDate startDate, LocalDate endDate) {
        Campaign campaign = new Campaign(brand, title);
        campaign.setDescription(description);
        campaign.setObjectives("Build measurable awareness with clearly labelled demonstration content.");
        campaign.setProductDetails("Fictional product or service used to demonstrate the campaign workspace.");
        campaign.setTargetAudience("Adults interested in " + niches.toLowerCase(java.util.Locale.ROOT) + ".");
        campaign.setPlatforms(platforms);
        campaign.setNiches(niches);
        campaign.setContentFormats(contentFormats);
        campaign.setStartDate(startDate);
        campaign.setEndDate(endDate);
        campaign.setApplicationDeadline(deadline);
        campaign.setMinBudget(minBudget);
        campaign.setMaxBudget(maxBudget);
        campaign.setCurrency("USD");
        campaign.setCompensationModel(CompensationModel.FIXED_FEE);
        campaign.setCreatorsRequired(creatorsRequired);
        campaign.setDeliverableRequirements("One primary asset per creator plus supporting stories, "
                + "submitted through the collaboration workspace.");
        campaign.setReviewRules("The brand reviews each submission and may request one revision round.");
        campaign.setCategory(category);
        campaign.setStatus(CampaignStatus.PUBLISHED);
        campaignRepository.save(campaign);
    }
}
