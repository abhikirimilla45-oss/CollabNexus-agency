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

class PaymentApiTest extends AbstractApiTest {

    @Test
    void milestonesFollowValidatedTransitionsAndNotifyBothParties() throws Exception {
        Workspace workspace = workspace();
        long paymentId = addMilestone(workspace.brand(), workspace.collaborationId(),
                "Deposit", "1000.00", LocalDate.now().toString());

        mockMvc.perform(get("/api/payments/" + paymentId).session(workspace.creator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DUE"))
                .andExpect(jsonPath("$.data.remainingAmount").value(1000.0));

        mockMvc.perform(get("/api/payments/summary").session(workspace.brand()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordCount").value(1))
                .andExpect(jsonPath("$.data.scheduledAmount").value(1000.0));

        mockMvc.perform(post("/api/payments/" + paymentId + "/status")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PAID"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/payments/" + paymentId + "/status")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DISPUTED", "notes", "Milestone does not match the agreed brief."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPUTED"));

        mockMvc.perform(post("/api/payments/" + paymentId + "/status")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PROCESSING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        mockMvc.perform(post("/api/payments/" + paymentId + "/status")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PAID", "externalReference", "RECORD-ONLY-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paidAmount").value(1000.0))
                .andExpect(jsonPath("$.data.remainingAmount").value(0.0));

        mockMvc.perform(post("/api/payments/" + paymentId + "/status")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DUE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_PAYMENT_TRANSITION"));

        JsonNode creatorNotifications = read(mockMvc.perform(get("/api/notifications")
                        .session(workspace.creator()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(creatorNotifications.path("data").path("content").findValuesAsText("type"))
                .contains("PAYMENT_UPDATED");

        JsonNode brandNotifications = read(mockMvc.perform(get("/api/notifications")
                        .session(workspace.brand()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(brandNotifications.path("data").path("content").findValuesAsText("type"))
                .contains("PAYMENT_UPDATED");
    }

    @Test
    void paymentRecordsRespectAgreedAmountsAndParticipantScope() throws Exception {
        Workspace workspace = workspace();
        addMilestone(workspace.brand(), workspace.collaborationId(),
                "First instalment", "1500.00", LocalDate.now().toString());

        mockMvc.perform(post("/api/brand/collaborations/" + workspace.collaborationId() + "/payments")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(paymentBody("Excess instalment", "1500.00", LocalDate.now().toString()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PAYMENT_EXCEEDS_AGREED_AMOUNT"));

        long paymentId = addMilestone(workspace.brand(), workspace.collaborationId(),
                "Second instalment", "1000.00", LocalDate.now().toString());

        mockMvc.perform(post("/api/brand/collaborations/" + workspace.collaborationId() + "/payments")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(paymentBody("Creator attempt", "100.00", LocalDate.now().toString()))))
                .andExpect(status().isForbidden());

        MockHttpSession otherBrand = registerBrand(email("other-brand"));
        mockMvc.perform(get("/api/payments/" + paymentId).session(otherBrand))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/brand/collaborations/" + workspace.collaborationId() + "/payments")
                        .session(otherBrand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(paymentBody("Foreign payment", "100.00", LocalDate.now().toString()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/payments").session(otherBrand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    private long addMilestone(MockHttpSession brand, long collaborationId, String milestone,
                              String amount, String dueDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/brand/collaborations/" + collaborationId + "/payments")
                        .session(brand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(paymentBody(milestone, amount, dueDate))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private Map<String, Object> paymentBody(String milestone, String amount, String dueDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("milestone", milestone);
        body.put("amount", amount);
        body.put("currency", "USD");
        body.put("dueDate", dueDate);
        body.put("notes", "Automated verification record. No money is transferred.");
        return body;
    }
}
