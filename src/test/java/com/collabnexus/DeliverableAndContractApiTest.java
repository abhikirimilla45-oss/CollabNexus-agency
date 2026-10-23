package com.collabnexus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliverableAndContractApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deliverableWorkflowTracksSubmissionsRevisionsAndApproval() throws Exception {
        Workspace workspace = workspace();
        long deliverableId = createDeliverable(workspace.brand, workspace.collaborationId,
                "Launch reel", LocalDate.now().plusDays(10));

        mockMvc.perform(get("/api/collaborations/" + workspace.collaborationId + "/deliverables")
                        .session(workspace.creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].campaignTitle").isNotEmpty());

        mockMvc.perform(post("/api/deliverables/" + deliverableId + "/start")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        MockMultipartFile file = new MockMultipartFile("file", "concept.pdf",
                "application/pdf", "%PDF-1.4 demo".getBytes(StandardCharsets.UTF_8));
        MvcResult firstSubmission = mockMvc.perform(multipart(
                                "/api/deliverables/" + deliverableId + "/submissions")
                        .file(file)
                        .param("submissionUrl", "https://example.com/reel")
                        .param("submissionNotes", "First cut for review.")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.revisionNumber").value(1))
                .andExpect(jsonPath("$.data.submissions.length()").value(1))
                .andExpect(jsonPath("$.data.submissions[0].hasFile").value(true))
                .andReturn();
        long submissionId = read(firstSubmission).path("data").path("submissions").get(0)
                .path("id").asLong();

        mockMvc.perform(get("/api/deliverables/" + deliverableId + "/submissions/"
                        + submissionId + "/file")
                        .session(workspace.creator))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("concept.pdf")));

        mockMvc.perform(post("/api/brand/deliverables/" + deliverableId + "/request-revision")
                        .session(workspace.brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comments", "Tighten the opening three seconds."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVISION_REQUESTED"))
                .andExpect(jsonPath("$.data.reviewComments")
                        .value("Tighten the opening three seconds."))
                .andExpect(jsonPath("$.data.reviews.length()").value(1))
                .andExpect(jsonPath("$.data.reviews[0].action").value("REVISION_REQUESTED"));

        mockMvc.perform(multipart("/api/deliverables/" + deliverableId + "/submissions")
                        .param("submissionUrl", "https://example.com/reel-v2")
                        .param("submissionNotes", "Revised opening.")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.revisionNumber").value(2))
                .andExpect(jsonPath("$.data.submissions.length()").value(2));

        mockMvc.perform(post("/api/brand/deliverables/" + deliverableId + "/approve")
                        .session(workspace.brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comments", "Approved for publication."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviews.length()").value(2));

        mockMvc.perform(get("/api/collaborations").session(workspace.brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].deliverableCount").value(1))
                .andExpect(jsonPath("$.data.content[0].approvedDeliverableCount").value(1))
                .andExpect(jsonPath("$.data.content[0].contractReference").isNotEmpty());
    }

    @Test
    void deliverableOwnershipAndStateTransitionsAreEnforced() throws Exception {
        Workspace workspace = workspace();
        MockHttpSession otherBrand = registerBrand(email("other-brand"));
        MockHttpSession otherCreator = registerCreator(email("other-creator"));
        long deliverableId = createDeliverable(workspace.brand, workspace.collaborationId,
                "Product photography", LocalDate.now().plusDays(7));

        mockMvc.perform(post("/api/brand/collaborations/" + workspace.collaborationId
                                + "/deliverables")
                        .session(otherBrand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(deliverableBody("Intrusion", LocalDate.now().plusDays(3)))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/deliverables/" + deliverableId).session(otherCreator))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/deliverables/" + deliverableId + "/start")
                        .session(otherCreator).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(multipart("/api/deliverables/" + deliverableId + "/submissions")
                        .param("submissionUrl", "https://example.com/work")
                        .session(workspace.brand).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/brand/deliverables/" + deliverableId + "/approve")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/brand/deliverables/" + deliverableId + "/approve")
                        .session(workspace.brand).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(multipart("/api/deliverables/" + deliverableId + "/submissions")
                        .param("submissionUrl", "https://example.com/work")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/api/deliverables/" + deliverableId + "/start")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/deliverables/" + deliverableId + "/start")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/brand/deliverables/" + deliverableId)
                        .session(workspace.brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(deliverableBody("Changed terms", LocalDate.now().plusDays(9)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DELIVERABLE_TERMS_LOCKED"));

        mockMvc.perform(get("/api/deliverables").session(otherBrand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void deliverableFiltersReportOverdueAndUpcomingDeadlines() throws Exception {
        Workspace workspace = workspace();
        createDeliverable(workspace.brand, workspace.collaborationId,
                "Late deliverable", LocalDate.now().minusDays(1));
        createDeliverable(workspace.brand, workspace.collaborationId,
                "Upcoming deliverable", LocalDate.now().plusDays(2));

        mockMvc.perform(get("/api/deliverables")
                        .session(workspace.brand)
                        .param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$.data.content[0].overdue").value(true));

        mockMvc.perform(get("/api/deliverables")
                        .session(workspace.creator)
                        .param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/deliverables")
                        .session(workspace.creator)
                        .param("upcomingDays", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    void supportingFileValidationAndAccessAreEnforced() throws Exception {
        Workspace workspace = workspace();
        MockHttpSession otherCreator = registerCreator(email("other-creator"));
        long deliverableId = createDeliverable(workspace.brand, workspace.collaborationId,
                "File review", LocalDate.now().plusDays(4));
        start(workspace.creator, deliverableId);

        MockMultipartFile executable = new MockMultipartFile("file", "payload.exe",
                "application/octet-stream", "MZ".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/deliverables/" + deliverableId + "/submissions")
                        .file(executable)
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FILE_TYPE_NOT_ALLOWED"));

        mockMvc.perform(multipart("/api/deliverables/" + deliverableId + "/submissions")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SUBMISSION_CONTENT_REQUIRED"));

        mockMvc.perform(multipart("/api/deliverables/" + deliverableId + "/submissions")
                        .param("submissionUrl", "not-a-url")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_URL"));

        MvcResult submitted = mockMvc.perform(multipart(
                                "/api/deliverables/" + deliverableId + "/submissions")
                        .param("submissionUrl", "https://example.com/private-work")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        long submissionId = read(submitted).path("data").path("submissions").get(0)
                .path("id").asLong();

        mockMvc.perform(get("/api/deliverables/" + deliverableId + "/submissions/"
                        + submissionId + "/file")
                        .session(workspace.creator))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/deliverables/" + deliverableId).session(otherCreator))
                .andExpect(status().isNotFound());
    }

    @Test
    void contractIsVersionedAcknowledgedAndLocked() throws Exception {
        Workspace workspace = workspace();
        assertThat(workspace.contractReference).startsWith("CN-");

        mockMvc.perform(get("/api/collaborations/" + workspace.collaborationId + "/contract")
                        .session(workspace.creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andExpect(jsonPath("$.data.versions.length()").value(1))
                .andExpect(jsonPath("$.data.agreedCompensation").value(2500));

        mockMvc.perform(get("/api/contracts").session(workspace.brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(put("/api/brand/contracts/" + workspace.contractId)
                        .session(workspace.brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contractBody(2750, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.versionNumber").value(2))
                .andExpect(jsonPath("$.data.versions.length()").value(2))
                .andExpect(jsonPath("$.data.revisionAllowance").value(2));

        MockHttpSession otherBrand = registerBrand(email("other-brand"));
        mockMvc.perform(get("/api/contracts/" + workspace.contractId).session(otherBrand))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/brand/contracts/" + workspace.contractId)
                        .session(workspace.creator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contractBody(2800, 1))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/brand/contracts/" + workspace.contractId + "/send")
                        .session(workspace.brand).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_ACKNOWLEDGEMENT"));

        mockMvc.perform(post("/api/contracts/" + workspace.contractId + "/acknowledge")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.creatorAcknowledged").value(true))
                .andExpect(jsonPath("$.data.brandAcknowledged").value(false))
                .andExpect(jsonPath("$.data.status").value("PENDING_ACKNOWLEDGEMENT"));

        mockMvc.perform(post("/api/contracts/" + workspace.contractId + "/acknowledge")
                        .session(workspace.creator).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_ACKNOWLEDGED"));

        mockMvc.perform(post("/api/contracts/" + workspace.contractId + "/acknowledge")
                        .session(workspace.brand).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandAcknowledged").value(true))
                .andExpect(jsonPath("$.data.creatorAcknowledged").value(true))
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.data.acceptances.length()").value(2));

        mockMvc.perform(put("/api/brand/contracts/" + workspace.contractId)
                        .session(workspace.brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(contractBody(3000, 3))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONTRACT_TERMS_LOCKED"));

        mockMvc.perform(post("/api/brand/contracts/" + workspace.contractId + "/send")
                        .session(workspace.brand).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONTRACT_TERMS_LOCKED"));
    }

    @Test
    void unauthenticatedDeliverableAndContractRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/deliverables"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/contracts"))
                .andExpect(status().isUnauthorized());
    }

    private Workspace workspace() throws Exception {
        MockHttpSession brand = registerBrand(email("brand"));
        MockHttpSession creator = registerCreator(email("creator"));
        long campaignId = createAndPublishCampaign(brand, 2);
        long applicationId = apply(creator, campaignId, 2500);
        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/accept")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());

        MvcResult collaborations = mockMvc.perform(get("/api/collaborations").session(creator))
                .andExpect(status().isOk())
                .andReturn();
        long collaborationId = read(collaborations).path("data").path("content").get(0)
                .path("id").asLong();

        MvcResult contract = mockMvc.perform(get("/api/collaborations/" + collaborationId
                                + "/contract")
                        .session(brand))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode contractData = read(contract).path("data");
        return new Workspace(brand, creator, campaignId, collaborationId,
                contractData.path("id").asLong(), contractData.path("referenceNumber").asText());
    }

    private long createDeliverable(MockHttpSession brand, long collaborationId, String title,
                                   LocalDate dueDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brand/collaborations/" + collaborationId
                                + "/deliverables")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(deliverableBody(title, dueDate))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private void start(MockHttpSession creator, long deliverableId) throws Exception {
        mockMvc.perform(post("/api/deliverables/" + deliverableId + "/start")
                        .session(creator).with(csrf()))
                .andExpect(status().isOk());
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

    private Map<String, Object> contractBody(int amount, int revisions) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("agreedCompensation", amount);
        body.put("currency", "USD");
        body.put("deliverablesSummary", "One launch reel and two stories.");
        body.put("completionDeadline", LocalDate.now().plusDays(45).toString());
        body.put("usageRights", "Paid usage for 90 days on brand-owned channels.");
        body.put("revisionAllowance", revisions);
        body.put("cancellationTerms", "Written cancellation with completed work payable.");
        body.put("paymentSchedule", "Manual milestone: 100% after approval.");
        return body;
    }

    private long apply(MockHttpSession creator, long campaignId, int price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns/" + campaignId + "/applications")
                        .session(creator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(applicationBody(price))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).path("data").path("id").asLong();
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
        MvcResult result = mockMvc.perform(post("/api/brand/campaigns")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(campaignBody(capacity))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = read(result).path("data").path("id").asLong();
        mockMvc.perform(post("/api/brand/campaigns/" + id + "/publish")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk());
        return id;
    }

    private Map<String, Object> campaignBody(int capacity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Phase six campaign " + UUID.randomUUID().toString().substring(0, 8));
        body.put("description", "A campaign that verifies deliverables and contracts.");
        body.put("objectives", "Test the phase six hand-off from collaboration to contract.");
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

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record Workspace(MockHttpSession brand, MockHttpSession creator, long campaignId,
                             long collaborationId, long contractId, String contractReference) {
    }
}
