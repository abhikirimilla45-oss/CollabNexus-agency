package com.collabnexus;

import com.collabnexus.users.Role;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractApiTest {

    protected static final String PASSWORD = "Demo!Pass1234";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected MockHttpSession registerBrand(String email) throws Exception {
        return register(Map.of(
                "fullName", "Jordan Ellis",
                "email", email,
                "password", PASSWORD,
                "role", "brand",
                "companyName", "Northwind Atelier " + suffix(),
                "companyWebsite", "https://example.com",
                "country", "United Kingdom"));
    }

    protected MockHttpSession registerCreator(String email) throws Exception {
        return register(Map.of(
                "fullName", "Maya Chen",
                "email", email,
                "password", PASSWORD,
                "role", "creator",
                "primaryNiche", "Beauty & Skincare",
                "primaryPlatform", "Instagram",
                "country", "Singapore"));
    }

    protected MockHttpSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected MockHttpSession createAdmin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        userRepository.saveAndFlush(new User(email, passwordEncoder.encode(PASSWORD),
                "Platform Administrator", Role.ADMIN));
        return login(email);
    }

    protected long createCampaign(MockHttpSession brand, int capacity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(campaignBody(capacity))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    protected long createAndPublishCampaign(MockHttpSession brand, int capacity) throws Exception {
        long campaignId = createCampaign(brand, capacity);
        mockMvc.perform(post("/api/brand/campaigns/" + campaignId + "/publish")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());
        return campaignId;
    }

    protected long apply(MockHttpSession creator, long campaignId, int price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns/" + campaignId + "/applications")
                        .session(creator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(applicationBody(price))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    protected void accept(MockHttpSession brand, long applicationId) throws Exception {
        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/accept")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());
    }

    protected long collaborationId(MockHttpSession participant) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/collaborations").session(participant))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("content").get(0).path("id").asLong();
    }

    protected Workspace workspace() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession creator = registerCreator(email("creator"));
        long campaignId = createAndPublishCampaign(brand, 2);
        long applicationId = apply(creator, campaignId, 2500);
        accept(brand, applicationId);
        long collaborationId = collaborationId(creator);
        MvcResult contract = mockMvc.perform(get("/api/collaborations/" + collaborationId + "/contract")
                        .session(brand))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode contractData = read(contract).path("data");
        return new Workspace(brand, creator, campaignId, applicationId, collaborationId,
                contractData.path("id").asLong());
    }

    protected long createDeliverable(MockHttpSession brand, long collaborationId, String title,
                                     LocalDate dueDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brand/collaborations/" + collaborationId
                                + "/deliverables")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(deliverableBody(title, dueDate))))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    protected void startDeliverable(MockHttpSession creator, long deliverableId) throws Exception {
        mockMvc.perform(post("/api/deliverables/" + deliverableId + "/start")
                        .session(creator).with(csrf()))
                .andExpect(status().isOk());
    }

    protected long submitDeliverable(MockHttpSession creator, long deliverableId, String url) throws Exception {
        MvcResult result = mockMvc.perform(multipart(
                                "/api/deliverables/" + deliverableId + "/submissions")
                        .session(creator).with(csrf())
                        .param("submissionUrl", url)
                        .param("submissionNotes", "Automated verification submission."))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("submissions").get(0).path("id").asLong();
    }

    protected void approveDeliverable(MockHttpSession brand, long deliverableId) throws Exception {
        mockMvc.perform(post("/api/brand/deliverables/" + deliverableId + "/approve")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comments", "Approved during automated verification."))))
                .andExpect(status().isOk());
    }

    protected void startCollaboration(MockHttpSession participant, long collaborationId) throws Exception {
        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/start")
                        .session(participant).with(csrf()))
                .andExpect(status().isOk());
    }

    protected void completeCollaboration(MockHttpSession participant, long collaborationId) throws Exception {
        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/complete")
                        .session(participant).with(csrf()))
                .andExpect(status().isOk());
    }

    protected long createPayment(MockHttpSession brand, long collaborationId, String milestone,
                                 int amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brand/collaborations/" + collaborationId + "/payments")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(paymentBody(milestone, amount))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    protected String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
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

    private Map<String, Object> campaignBody(int capacity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Campaign " + suffix());
        body.put("description", "A campaign created for automated final-stage verification.");
        body.put("objectives", "Verify the complete collaboration workflow.");
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

    private Map<String, Object> applicationBody(int price) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("proposal", "A considered proposal for the campaign.");
        body.put("proposedPrice", price);
        body.put("currency", "USD");
        body.put("availableFrom", LocalDate.now().plusDays(14).toString());
        body.put("portfolioUrl", "https://example.com/portfolio");
        return body;
    }

    private Map<String, Object> deliverableBody(String title, LocalDate dueDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "A clear deliverable for automated verification.");
        body.put("platform", "Instagram");
        body.put("contentFormat", "Short-form video");
        body.put("dueDate", dueDate.toString());
        return body;
    }

    private Map<String, Object> paymentBody(String milestone, int amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("milestone", milestone);
        body.put("amount", amount);
        body.put("currency", "USD");
        body.put("dueDate", LocalDate.now().toString());
        body.put("notes", "Recorded for automated verification. No money is transferred.");
        return body;
    }

    protected record Workspace(MockHttpSession brand, MockHttpSession creator, long campaignId,
                               long applicationId, long collaborationId, long contractId) {
    }
}
