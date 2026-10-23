package com.collabnexus;

import com.collabnexus.applications.ApplicationService;
import com.collabnexus.applications.CampaignApplicationRepository;
import com.collabnexus.applications.ApplicationStatus;
import com.collabnexus.campaigns.Campaign;
import com.collabnexus.campaigns.CampaignRepository;
import com.collabnexus.common.ApiException;
import com.collabnexus.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignApplicationRepository applicationRepository;

    @Autowired
    private ApplicationService applicationService;

    @Test
    void creatorAppliesAndBrandAcceptsCreatingCollaboration() throws Exception {
        String brandEmail = email("brand");
        String creatorEmail = email("creator");
        MockHttpSession brand = registerBrand(brandEmail);
        MockHttpSession creator = registerCreator(creatorEmail);
        long campaignId = createAndPublishCampaign(brand, 2);
        long applicationId = apply(creator, campaignId, 2500);

        mockMvc.perform(get("/api/creator/applications").session(creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].campaignTitle").isNotEmpty());

        mockMvc.perform(get("/api/brand/applications")
                        .session(brand)
                        .param("campaignId", String.valueOf(campaignId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].creatorName").value("Demo Creator"))
                .andExpect(jsonPath("$.data.content[0].creatorNiche").value("Beauty & Skincare"))
                .andExpect(jsonPath("$.data.content[0].proposal").value("A considered proposal for the campaign."));

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/shortlist")
                        .session(brand)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/accept")
                        .session(brand)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/collaborations").session(creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.content[0].agreedAmount").value(2500))
                .andExpect(jsonPath("$.data.content[0].creatorName").value("Demo Creator"))
                .andExpect(jsonPath("$.data.content[0].brandName").isNotEmpty());

        mockMvc.perform(get("/api/collaborations").session(brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].applicationId").value(applicationId));
    }

    @Test
    void duplicateApplicationIsRejected() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession creator = registerCreator(email("creator"));
        long campaignId = createAndPublishCampaign(brand, 2);

        apply(creator, campaignId, 1200);

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/applications")
                        .session(creator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(applicationBody(1500))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_APPLICATION"));
    }

    @Test
    void campaignsThatAreNotPublishedRejectApplications() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession creator = registerCreator(email("creator"));
        long draftId = createCampaign(brand, 2);

        mockMvc.perform(post("/api/campaigns/" + draftId + "/applications")
                        .session(creator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(applicationBody(1500))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CAMPAIGN_NOT_ACCEPTING_APPLICATIONS"));

        long publishedId = createAndPublishCampaign(brand, 2);
        mockMvc.perform(post("/api/brand/campaigns/" + publishedId + "/close")
                        .session(brand).with(csrf())).andExpect(status().isOk());

        mockMvc.perform(post("/api/campaigns/" + publishedId + "/applications")
                        .session(creator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(applicationBody(1500))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CAMPAIGN_NOT_ACCEPTING_APPLICATIONS"));
    }

    @Test
    void capacityStopsExtraAcceptances() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession first = registerCreator(email("first"));
        MockHttpSession second = registerCreator(email("second"));
        long campaignId = createAndPublishCampaign(brand, 1);
        long firstApplication = apply(first, campaignId, 1000);
        long secondApplication = apply(second, campaignId, 1100);

        mockMvc.perform(post("/api/brand/applications/" + firstApplication + "/accept")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/brand/applications/" + secondApplication + "/accept")
                        .session(brand).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CAMPAIGN_CAPACITY_REACHED"));
    }

    @Test
    void invalidApplicationTransitionsAreRejected() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession creator = registerCreator(email("creator"));
        long campaignId = createAndPublishCampaign(brand, 2);
        long applicationId = apply(creator, campaignId, 1000);

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/accept")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/shortlist")
                        .session(brand).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/reject")
                        .session(brand).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/api/creator/applications/" + applicationId + "/withdraw")
                        .session(creator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void withdrawWorksBeforeTheDeadline() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession creator = registerCreator(email("creator"));
        long campaignId = createAndPublishCampaign(brand, 2);
        long applicationId = apply(creator, campaignId, 1000);

        mockMvc.perform(post("/api/creator/applications/" + applicationId + "/withdraw")
                        .session(creator).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    @Test
    void brandsAndCreatorsCannotActOnOtherPeoplesRecords() throws Exception {
        MockHttpSession ownerBrand = registerBrand(email("owner-brand"));
        MockHttpSession otherBrand = registerBrand(email("other-brand"));
        MockHttpSession ownerCreator = registerCreator(email("owner-creator"));
        MockHttpSession otherCreator = registerCreator(email("other-creator"));

        long campaignId = createAndPublishCampaign(ownerBrand, 2);
        long applicationId = apply(ownerCreator, campaignId, 1000);

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/accept")
                        .session(otherBrand).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/brand/applications/" + applicationId).session(otherBrand))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/creator/applications/" + applicationId + "/withdraw")
                        .session(otherCreator).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/creator/applications/" + applicationId).session(otherCreator))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/brand/applications").session(ownerCreator))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/creator/applications").session(ownerBrand))
                .andExpect(status().isForbidden());
    }

    @Test
    void collaborationTransitionsAreValidatedAndOwnershipIsEnforced() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession creator = registerCreator(email("creator"));
        MockHttpSession otherBrand = registerBrand(email("other-brand"));
        long campaignId = createAndPublishCampaign(brand, 2);
        long applicationId = apply(creator, campaignId, 1800);

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/accept")
                        .session(brand).with(csrf())).andExpect(status().isOk());

        mockMvc.perform(get("/api/collaborations").session(otherBrand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        long collaborationId = collaborationId(creator);

        mockMvc.perform(get("/api/collaborations/" + collaborationId).session(otherBrand))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/complete")
                        .session(creator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/start")
                        .session(creator).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/start")
                        .session(creator).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/complete")
                        .session(creator).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/cancel")
                        .session(brand).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void concurrentAcceptsNeverExceedCampaignCapacity() throws Exception {
        String brandEmail = email("brand");
        MockHttpSession brand = registerBrand(brandEmail);
        MockHttpSession first = registerCreator(email("first"));
        MockHttpSession second = registerCreator(email("second"));
        long campaignId = createAndPublishCampaign(brand, 1);
        long firstApplication = apply(first, campaignId, 1000);
        long secondApplication = apply(second, campaignId, 1100);
        Long brandUserId = userRepository.findByEmailIgnoreCase(brandEmail).orElseThrow().getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> one = pool.submit(() -> acceptAfterBarrier(brandUserId, firstApplication, ready, start));
            Future<Boolean> two = pool.submit(() -> acceptAfterBarrier(brandUserId, secondApplication, ready, start));
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();

            int successes = (one.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (two.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        long accepted = applicationRepository.countByCampaignIdAndStatus(campaign.getId(),
                ApplicationStatus.ACCEPTED);
        assertThat(accepted).isEqualTo(1);
        assertThat(accepted).isLessThanOrEqualTo(campaign.getCreatorsRequired());
    }

    private boolean acceptAfterBarrier(Long brandUserId, Long applicationId,
                                       CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            applicationService.accept(brandUserId, applicationId);
            return true;
        } catch (ApiException ex) {
            assertThat(ex.getCode()).isEqualTo("CAMPAIGN_CAPACITY_REACHED");
            return false;
        }
    }

    private long collaborationId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/collaborations").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();
    }

    private long apply(MockHttpSession creator, long campaignId, int price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns/" + campaignId + "/applications")
                        .session(creator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(applicationBody(price))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Map<String, Object> applicationBody(int price) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("proposal", "A considered proposal for the campaign.");
        body.put("proposedPrice", price);
        body.put("currency", "USD");
        body.put("availableFrom", LocalDate.now().plusDays(14).toString());
        body.put("portfolioUrl", "https://example.com/portfolio");
        return body;
    }

    private long createAndPublishCampaign(MockHttpSession brand, int capacity) throws Exception {
        long id = createCampaign(brand, capacity);
        mockMvc.perform(post("/api/brand/campaigns/" + id + "/publish")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());
        return id;
    }

    private long createCampaign(MockHttpSession brand, int capacity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(campaignBody(capacity))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Map<String, Object> campaignBody(int capacity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Phase five campaign " + UUID.randomUUID().toString().substring(0, 8));
        body.put("description", "A campaign that verifies the application and collaboration workflow.");
        body.put("objectives", "Test the marketplace hand-off from application to collaboration.");
        body.put("productDetails", "Fictional product for automated verification.");
        body.put("targetAudience", "Adults interested in the test niche.");
        body.put("platforms", "Instagram");
        body.put("niches", "Beauty & Skincare");
        body.put("contentFormats", "Short-form video");
        body.put("startDate", LocalDate.now().plusDays(10).toString());
        body.put("endDate", LocalDate.now().plusDays(60).toString());
        body.put("applicationDeadline", LocalDate.now().plusDays(7).toString());
        body.put("minBudget", 1000);
        body.put("maxBudget", 3000);
        body.put("currency", "USD");
        body.put("compensationModel", "FIXED_FEE");
        body.put("creatorsRequired", capacity);
        body.put("deliverableRequirements", "One short-form video and two stories.");
        body.put("reviewRules", "One revision round.");
        body.put("category", "Awareness");
        return body;
    }

    private MockHttpSession registerBrand(String email) throws Exception {
        return register(Map.of(
                "fullName", "Jordan Ellis",
                "email", email,
                "password", "Demo!Pass1234",
                "role", "brand",
                "companyName", "Northwind Atelier " + UUID.randomUUID().toString().substring(0, 6),
                "companyWebsite", "https://example.com",
                "country", "United Kingdom"));
    }

    private MockHttpSession registerCreator(String email) throws Exception {
        return register(Map.of(
                "fullName", "Demo Creator",
                "email", email,
                "password", "Demo!Pass1234",
                "role", "creator",
                "primaryNiche", "Beauty & Skincare",
                "primaryPlatform", "Instagram",
                "country", "Singapore"));
    }

    private MockHttpSession register(Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
