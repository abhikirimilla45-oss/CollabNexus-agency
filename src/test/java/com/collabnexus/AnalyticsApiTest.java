package com.collabnexus;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsApiTest extends AbstractApiTest {

    @Test
    void dashboardsReportDefinitionBasedCountsForEachRole() throws Exception {
        Workspace workspace = workspace();

        mockMvc.perform(get("/api/brand/analytics/overview").session(workspace.brand()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCampaigns").value(1))
                .andExpect(jsonPath("$.data.totalApplications").value(1))
                .andExpect(jsonPath("$.data.acceptedApplications").value(1))
                .andExpect(jsonPath("$.data.totalCollaborations").value(1))
                .andExpect(jsonPath("$.data.monthlyApplications").isMap());

        mockMvc.perform(get("/api/creator/analytics/overview").session(workspace.creator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalApplications").value(1))
                .andExpect(jsonPath("$.data.acceptedApplications").value(1))
                .andExpect(jsonPath("$.data.totalCollaborations").value(1));

        mockMvc.perform(get("/api/creator/analytics/overview").session(workspace.brand()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/brand/analytics/overview").session(workspace.creator()))
                .andExpect(status().isForbidden());

        MockHttpSession admin = createAdmin();
        mockMvc.perform(get("/api/admin/analytics/overview").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.campaignStatus").isMap());
        mockMvc.perform(get("/api/admin/analytics/overview").session(workspace.brand()))
                .andExpect(status().isForbidden());
    }

    @Test
    void manualCampaignResultsAreValidatedAndScopedToTheOwningBrand() throws Exception {
        Workspace workspace = workspace();

        MvcResult created = mockMvc.perform(post("/api/brand/campaigns/" + workspace.campaignId() + "/metrics")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(metricBody(1500L, 40L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reach").value(1500))
                .andExpect(jsonPath("$.data.clicks").value(40))
                .andReturn();
        long metricId = read(created).path("data").path("id").asLong();

        mockMvc.perform(get("/api/brand/campaigns/" + workspace.campaignId() + "/metrics")
                        .session(workspace.brand()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(metricId));

        mockMvc.perform(get("/api/brand/analytics/manual-results").session(workspace.brand()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reach").value(1500))
                .andExpect(jsonPath("$.data.clicks").value(40));

        mockMvc.perform(post("/api/brand/campaigns/" + workspace.campaignId() + "/metrics")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(metricBody(-5L, 10L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("recordedOn", LocalDate.now().toString());
        mockMvc.perform(post("/api/brand/campaigns/" + workspace.campaignId() + "/metrics")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(empty)))
                .andExpect(status().isBadRequest());

        MockHttpSession otherBrand = registerBrand(email("other-brand"));
        mockMvc.perform(get("/api/brand/campaigns/" + workspace.campaignId() + "/metrics")
                        .session(otherBrand))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/brand/campaigns/" + workspace.campaignId() + "/metrics")
                        .session(otherBrand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(metricBody(100L, 5L))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/brand/campaigns/" + workspace.campaignId() + "/metrics")
                        .session(workspace.creator()))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> metricBody(Long reach, Long clicks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recordedOn", LocalDate.now().toString());
        body.put("reach", reach);
        body.put("clicks", clicks);
        body.put("spend", "125.50");
        body.put("source", "Automated verification");
        return body;
    }
}
