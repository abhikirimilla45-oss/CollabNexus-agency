package com.collabnexus;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the twelve-step collaboration journey described in the project brief
 * from registration through to eligible two-way reviews.
 */
class FullWorkflowApiTest extends AbstractApiTest {

    @Test
    void completeMarketplaceJourneyWorksEndToEnd() throws Exception {
        MockHttpSession brand = registerBrand(email("journey-brand"));
        MockHttpSession creator = registerCreator(email("journey-creator"));

        long campaignId = createAndPublishCampaign(brand, 1);
        long applicationId = apply(creator, campaignId, 2500);

        mockMvc.perform(post("/api/brand/applications/" + applicationId + "/shortlist")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));

        accept(brand, applicationId);
        long collaborationId = collaborationId(creator);

        MvcResult contractResult = mockMvc.perform(
                        get("/api/collaborations/" + collaborationId + "/contract").session(brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceNumber").isNotEmpty())
                .andReturn();
        JsonNode contract = read(contractResult).path("data");
        long contractId = contract.path("id").asLong();
        assertThat(contract.path("referenceNumber").asText()).startsWith("CN-");

        mockMvc.perform(post("/api/brand/contracts/" + contractId + "/send")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_ACKNOWLEDGEMENT"));

        mockMvc.perform(post("/api/contracts/" + contractId + "/acknowledge")
                        .session(creator).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/contracts/" + contractId + "/acknowledge")
                        .session(brand).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));

        startCollaboration(creator, collaborationId);

        long deliverableId = createDeliverable(brand, collaborationId, "Journey launch reel",
                LocalDate.now().plusDays(14));
        startDeliverable(creator, deliverableId);
        submitDeliverable(creator, deliverableId, "https://example.com/journey-v1");

        mockMvc.perform(post("/api/brand/deliverables/" + deliverableId + "/request-revision")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comments", "Add the product name in the first frame."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVISION_REQUESTED"));

        submitDeliverable(creator, deliverableId, "https://example.com/journey-v2");
        approveDeliverable(brand, deliverableId);

        long paymentId = createPayment(brand, collaborationId, "Full fee", 2500);
        Map<String, Object> paid = new LinkedHashMap<>();
        paid.put("status", "PAID");
        paid.put("paidAmount", 2500);
        paid.put("externalReference", "BANK-TRANSFER-DEMO-001");
        paid.put("notes", "Recorded manually for automated verification.");
        mockMvc.perform(post("/api/payments/" + paymentId + "/status")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(paid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        completeCollaboration(brand, collaborationId);

        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/reviews")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 5, "comment", "Clear brief and on-time delivery."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5));

        mockMvc.perform(post("/api/collaborations/" + collaborationId + "/reviews")
                        .session(creator).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 4, "comment", "Prompt feedback and fair terms."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(4));

        mockMvc.perform(get("/api/reviews/received/summary").session(brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(4));

        mockMvc.perform(get("/api/reviews/received/summary").session(creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(5));

        mockMvc.perform(get("/api/collaborations/" + collaborationId).session(creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.approvedDeliverableCount").value(1));
    }
}
