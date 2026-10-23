package com.collabnexus;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void brandCreatesDraftThatStaysOutOfPublicDiscovery() throws Exception {
        MockHttpSession brand = registerBrand();
        String title = uniqueTitle("Draft campaign");

        MvcResult created = mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(campaignBody(title, "Instagram", 1000, 2000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value(title))
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/campaigns").param("q", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/campaigns/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishedCampaignAppearsInDiscoveryAndPublicDetail() throws Exception {
        MockHttpSession brand = registerBrand();
        String title = uniqueTitle("Published campaign");
        long id = createCampaign(brand, campaignBody(title, "TikTok", 1500, 3000));

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/publish")
                        .session(brand)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/campaigns").param("q", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(id))
                .andExpect(jsonPath("$.data.content[0].status").value("PUBLISHED"));

        mockMvc.perform(get("/api/campaigns/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandName").isNotEmpty());
    }

    @Test
    void campaignLifecycleRejectsInvalidTransitions() throws Exception {
        MockHttpSession brand = registerBrand();
        long id = createCampaign(brand, campaignBody(uniqueTitle("Lifecycle"), "Instagram", 500, 900));

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/pause").session(brand).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/publish").session(brand).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/publish").session(brand).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/pause").session(brand).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAUSED"));

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/pause").session(brand).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/close").session(brand).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(put("/api/brand/campaigns/" + id)
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(campaignBody(uniqueTitle("Edit closed"), "Instagram", 500, 900))))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidDatesAndBudgetsAreRejectedOnTheServer() throws Exception {
        MockHttpSession brand = registerBrand();
        Map<String, Object> dates = campaignBody(uniqueTitle("Bad dates"), "Instagram", 500, 900);
        dates.put("endDate", LocalDate.now().plusDays(5).toString());
        dates.put("startDate", LocalDate.now().plusDays(10).toString());

        mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dates)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DATES"));

        Map<String, Object> budgets = campaignBody(uniqueTitle("Bad budget"), "Instagram", 5000, 1000);

        mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(budgets)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_BUDGET"));
    }

    @Test
    void brandsCannotEditOrPublishAnotherBrandsCampaign() throws Exception {
        MockHttpSession owner = registerBrand();
        MockHttpSession other = registerBrand();
        String title = uniqueTitle("Protected campaign");
        long id = createCampaign(owner, campaignBody(title, "Instagram", 700, 1200));

        mockMvc.perform(put("/api/brand/campaigns/" + id)
                        .session(other)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(campaignBody(title + " changed", "Instagram", 700, 1200))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/brand/campaigns/" + id + "/publish")
                        .session(other)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/brand/campaigns").session(other).param("q", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void creatorCannotUseBrandCampaignEndpoints() throws Exception {
        MockHttpSession creator = registerCreator();

        mockMvc.perform(post("/api/brand/campaigns")
                        .session(creator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(campaignBody(uniqueTitle("Creator attempt"), "Instagram", 500, 900))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/brand/campaigns").session(creator))
                .andExpect(status().isForbidden());
    }

    @Test
    void discoverySupportsPlatformBudgetAndPaginationFilters() throws Exception {
        MockHttpSession brand = registerBrand();
        String platformToken = "Platform" + UUID.randomUUID().toString().substring(0, 8);

        long matching = createCampaign(brand,
                campaignBody(uniqueTitle("Matching"), platformToken, 2500, 5000));
        mockMvc.perform(post("/api/brand/campaigns/" + matching + "/publish")
                        .session(brand).with(csrf())).andExpect(status().isOk());

        createCampaign(brand, campaignBody(uniqueTitle("No match"), "LinkedIn", 100, 200));

        mockMvc.perform(get("/api/campaigns")
                        .param("platform", platformToken)
                        .param("minBudget", "3000")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(matching));
    }

    @Test
    void publicCreatorDirectoryIsPaginatedAndNeverExposesEmail() throws Exception {
        registerCreator();

        mockMvc.perform(get("/api/creators").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].user").doesNotExist());
    }

    private long createCampaign(MockHttpSession brand, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Map<String, Object> campaignBody(String title, String platform, int minBudget, int maxBudget) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A demonstration brief created by the campaign API test suite.");
        body.put("objectives", "Measure application handling and lifecycle rules.");
        body.put("productDetails", "Fictional product for automated verification.");
        body.put("targetAudience", "Adults interested in the test niche.");
        body.put("platforms", platform);
        body.put("niches", "Beauty & Skincare");
        body.put("contentFormats", "Short-form video");
        body.put("startDate", LocalDate.now().plusDays(10).toString());
        body.put("endDate", LocalDate.now().plusDays(60).toString());
        body.put("applicationDeadline", LocalDate.now().plusDays(7).toString());
        body.put("minBudget", minBudget);
        body.put("maxBudget", maxBudget);
        body.put("currency", "USD");
        body.put("compensationModel", "FIXED_FEE");
        body.put("creatorsRequired", 2);
        body.put("deliverableRequirements", "One short-form video and two stories.");
        body.put("reviewRules", "One revision round.");
        body.put("category", "Awareness");
        return body;
    }

    private String uniqueTitle(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private MockHttpSession registerBrand() throws Exception {
        return register("brand", Map.of(
                "fullName", "Jordan Ellis",
                "email", "brand-" + UUID.randomUUID() + "@example.com",
                "password", "Demo!Pass1234",
                "role", "brand",
                "companyName", "Northwind Atelier " + UUID.randomUUID().toString().substring(0, 6),
                "companyWebsite", "https://example.com",
                "country", "United Kingdom"));
    }

    private MockHttpSession registerCreator() throws Exception {
        return register("creator", Map.of(
                "fullName", "Demo Creator",
                "email", "creator-" + UUID.randomUUID() + "@example.com",
                "password", "Demo!Pass1234",
                "role", "creator",
                "primaryNiche", "Beauty & Skincare",
                "primaryPlatform", "Instagram",
                "country", "Singapore"));
    }

    private MockHttpSession register(String label, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        if (session == null) {
            throw new IllegalStateException("Registration did not create a session for " + label);
        }
        return session;
    }
}
