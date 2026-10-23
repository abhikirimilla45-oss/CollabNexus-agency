package com.collabnexus;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewModerationApiTest extends AbstractApiTest {

    @Test
    void reviewsRequireCompletionAndAreModeratedWithAnAuditTrail() throws Exception {
        Workspace workspace = workspace();

        mockMvc.perform(post("/api/collaborations/" + workspace.collaborationId() + "/reviews")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 5, "comment", "Too early."))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COLLABORATION_NOT_COMPLETED"));

        startCollaboration(workspace.brand(), workspace.collaborationId());
        completeCollaboration(workspace.brand(), workspace.collaborationId());

        MvcResult brandReview = mockMvc.perform(post("/api/collaborations/" + workspace.collaborationId() + "/reviews")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 5, "comment", "Clear communication and on-time delivery."))))
                .andExpect(status().isCreated())
                .andReturn();
        long brandReviewId = read(brandReview).path("data").path("id").asLong();

        mockMvc.perform(post("/api/collaborations/" + workspace.collaborationId() + "/reviews")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 4))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_REVIEW"));

        mockMvc.perform(post("/api/collaborations/" + workspace.collaborationId() + "/reviews")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 4, "comment", "A clear brief and prompt feedback."))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/received/summary").session(workspace.brand()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(4.0));
        mockMvc.perform(get("/api/reviews/received/summary").session(workspace.creator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(5.0));

        long creatorProfileId = read(mockMvc.perform(get("/api/creator/profile").session(workspace.creator()))
                .andExpect(status().isOk()).andReturn()).path("data").path("id").asLong();
        mockMvc.perform(get("/api/reviews/creator/" + creatorProfileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].rating").value(5));

        MockHttpSession admin = createAdmin();
        mockMvc.perform(post("/api/admin/reviews/" + brandReviewId + "/status")
                        .session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mockMvc.perform(get("/api/reviews/received/summary").session(workspace.creator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(0));

        mockMvc.perform(get("/api/admin/audit-logs").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].action").value("REVIEW_HIDDEN"));

        mockMvc.perform(get("/api/admin/reviews").session(workspace.brand()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsRequireNotesBeforeClosureAndSuspensionInvalidatesSessions() throws Exception {
        Workspace workspace = workspace();
        long brandUserId = currentUserId(workspace.brand());

        Map<String, Object> reportBody = new LinkedHashMap<>();
        reportBody.put("targetType", "USER");
        reportBody.put("targetId", brandUserId);
        reportBody.put("reason", "Misleading campaign information");
        reportBody.put("details", "Automated verification report for the moderation queue.");
        MvcResult report = mockMvc.perform(post("/api/reports")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reportBody)))
                .andExpect(status().isOk())
                .andReturn();
        long reportId = read(report).path("data").path("id").asLong();

        MockHttpSession admin = createAdmin();
        mockMvc.perform(get("/api/admin/reports?status=OPEN").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reason").value("Misleading campaign information"));

        mockMvc.perform(post("/api/admin/reports/" + reportId + "/resolve")
                        .session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "RESOLVED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RESOLUTION_NOTES_REQUIRED"));

        mockMvc.perform(post("/api/admin/reports/" + reportId + "/resolve")
                        .session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "RESOLVED",
                                "adminNotes", "Reviewed the campaign and asked the brand to update the brief."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedByName").isNotEmpty());

        mockMvc.perform(post("/api/admin/users/" + brandUserId + "/suspend")
                        .session(admin).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/auth/me").session(workspace.brand()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SUSPENDED"));

        mockMvc.perform(post("/api/admin/users/" + brandUserId + "/activate")
                        .session(admin).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        JsonNode audit = read(mockMvc.perform(get("/api/admin/audit-logs").session(admin))
                .andExpect(status().isOk()).andReturn());
        assertThat(audit.path("data").path("content").findValuesAsText("action"))
                .contains("REPORT_RESOLVED", "USER_SUSPENDED", "USER_ACTIVATED");
    }

    private long currentUserId(MockHttpSession session) throws Exception {
        return read(mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andReturn()).path("data").path("user").path("id").asLong();
    }
}
