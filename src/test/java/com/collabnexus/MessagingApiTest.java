package com.collabnexus;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessagingApiTest extends AbstractApiTest {

    @Test
    void conversationsAreScopedToParticipantsAndTrackUnreadMessages() throws Exception {
        Workspace workspace = workspace();
        MvcResult created = mockMvc.perform(post("/api/conversations")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("collaborationId", workspace.collaborationId(),
                                "subject", "Launch coordination"))))
                .andExpect(status().isOk())
                .andReturn();
        long conversationId = read(created).path("data").path("id").asLong();

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .session(workspace.brand()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("body", "The brief is ready for your review."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderRole").value("BRAND"));

        mockMvc.perform(get("/api/conversations/unread-count").session(workspace.creator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        MvcResult messages = mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .session(workspace.creator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].body").value("The brief is ready for your review."))
                .andReturn();
        assertThat(read(messages).path("data").path("content").size()).isEqualTo(1);

        mockMvc.perform(get("/api/conversations/unread-count").session(workspace.creator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("body", "Thanks. I will review it today."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderRole").value("CREATOR"));

        mockMvc.perform(get("/api/conversations/unread-count").session(workspace.brand()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        mockMvc.perform(get("/api/conversations").session(workspace.brand()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].lastMessage").value("Thanks. I will review it today."));

        MockHttpSession otherBrand = registerBrand(email("other-brand"));
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages").session(otherBrand))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .session(otherBrand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("body", "Unauthorised message."))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/conversations")
                        .session(otherBrand).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("collaborationId", workspace.collaborationId()))))
                .andExpect(status().isNotFound());

        MockHttpSession admin = createAdmin();
        mockMvc.perform(get("/api/conversations").session(admin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("body", "Administrative note."))))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyMessagesAreRejected() throws Exception {
        Workspace workspace = workspace();
        MvcResult created = mockMvc.perform(post("/api/conversations")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("collaborationId", workspace.collaborationId()))))
                .andExpect(status().isOk())
                .andReturn();
        long conversationId = read(created).path("data").path("id").asLong();

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .session(workspace.creator()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("body", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}
