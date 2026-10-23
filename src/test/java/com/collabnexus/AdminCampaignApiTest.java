package com.collabnexus;

import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCampaignApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void administratorReviewsAndClosesACampaignWithAnAuditTrail() throws Exception {
        MockHttpSession admin = adminSession();
        MockHttpSession brand = registerBrand();
        String title = "Oversight " + UUID.randomUUID().toString().substring(0, 8);
        long campaignId = createAndPublishCampaign(brand, title);

        mockMvc.perform(get("/api/admin/campaigns")
                        .session(admin)
                        .param("q", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(campaignId))
                .andExpect(jsonPath("$.data.content[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.content[0].brandName").isNotEmpty());

        mockMvc.perform(post("/api/admin/campaigns/" + campaignId + "/close")
                        .session(admin)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(get("/api/admin/campaigns")
                        .session(admin)
                        .param("q", title)
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/admin/audit-logs").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].action").value("CAMPAIGN_CLOSED"));

        mockMvc.perform(get("/api/admin/campaigns").session(brand))
                .andExpect(status().isForbidden());
    }

    @Test
    void closingAnAlreadyClosedCampaignIsIdempotent() throws Exception {
        MockHttpSession admin = adminSession();
        MockHttpSession brand = registerBrand();
        String title = "Idempotent " + UUID.randomUUID().toString().substring(0, 8);
        long campaignId = createAndPublishCampaign(brand, title);

        mockMvc.perform(post("/api/admin/campaigns/" + campaignId + "/close")
                        .session(admin).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/campaigns/" + campaignId + "/close")
                        .session(admin).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    private MockHttpSession adminSession() throws Exception {
        userRepository.findByEmailIgnoreCase("admin@test.collabnexus")
                .orElseGet(() -> userRepository.save(new User("admin@test.collabnexus",
                        passwordEncoder.encode("Demo!Pass1234"), "Test Administrator", Role.ADMIN)));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@test.collabnexus",
                                "password", "Demo!Pass1234"))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long createAndPublishCampaign(MockHttpSession brand, String title) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campaignBody(title))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/brand/campaigns/" + id + "/publish")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());
        return id;
    }

    private Map<String, Object> campaignBody(String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A campaign created by the administrative oversight test suite.");
        body.put("objectives", "Verify platform moderation actions.");
        body.put("productDetails", "Fictional product for automated verification.");
        body.put("targetAudience", "Adults interested in the test niche.");
        body.put("platforms", "Instagram");
        body.put("niches", "Beauty & Skincare");
        body.put("contentFormats", "Short-form video");
        body.put("startDate", LocalDate.now().plusDays(10).toString());
        body.put("endDate", LocalDate.now().plusDays(60).toString());
        body.put("applicationDeadline", LocalDate.now().plusDays(7).toString());
        body.put("minBudget", 500);
        body.put("maxBudget", 1200);
        body.put("currency", "USD");
        body.put("compensationModel", "FIXED_FEE");
        body.put("creatorsRequired", 1);
        body.put("deliverableRequirements", "One short-form video.");
        body.put("reviewRules", "One revision round.");
        body.put("category", "Awareness");
        return body;
    }

    private MockHttpSession registerBrand() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Jordan Ellis",
                                "email", "oversight-" + UUID.randomUUID() + "@example.com",
                                "password", "Demo!Pass1234",
                                "role", "brand",
                                "companyName", "Oversight Atelier",
                                "companyWebsite", "https://example.com",
                                "country", "United Kingdom"))))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
