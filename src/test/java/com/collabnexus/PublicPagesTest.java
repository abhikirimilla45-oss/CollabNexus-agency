package com.collabnexus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageIsPublicAndServesHtml() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Log in")))
                .andExpect(content().string(containsString("Where Brand Meets Influence")));
    }

    @Test
    void registrationPageIsPublicAndOffersBothRoles() throws Exception {
        mockMvc.perform(get("/register.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Create your account")))
                .andExpect(content().string(containsString("Brand or agency")))
                .andExpect(content().string(containsString("Create creator account")));
    }

    @Test
    void privacyPolicyIsPublic() throws Exception {
        mockMvc.perform(get("/privacy.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Privacy Policy")));
    }

    @Test
    void termsOfServiceArePublic() throws Exception {
        mockMvc.perform(get("/terms.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Terms of Service")));
    }

    @Test
    void contactPageIsPublicAndExplainsTheReviewFlow() throws Exception {
        mockMvc.perform(get("/contact.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Send a message")))
                .andExpect(content().string(containsString("A real review queue, not a black hole.")));
    }

    @Test
    void contactSubmissionIsStoredAndAcknowledged() throws Exception {
        mockMvc.perform(post("/api/public/contact")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alex Morgan",
                                  "email": "alex@example.com",
                                  "role": "BRAND",
                                  "topic": "CAMPAIGNS",
                                  "message": "I would like to understand how campaign applications are reviewed.",
                                  "consent": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.message").value(containsString("Thanks for reaching out")));
    }

    @Test
    void contactSubmissionRequiresConsentAndAMeaningfulMessage() throws Exception {
        mockMvc.perform(post("/api/public/contact")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "A",
                                  "email": "not-an-email",
                                  "role": "BRAND",
                                  "topic": "CAMPAIGNS",
                                  "message": "Too short",
                                  "consent": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void marketplaceDiscoveryPagesArePublic() throws Exception {
        mockMvc.perform(get("/campaigns.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Campaign discovery")))
                .andExpect(content().string(containsString("Apply filters")));

        mockMvc.perform(get("/creators.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Creator discovery")))
                .andExpect(content().string(containsString("Public previews show only")));
    }
}
