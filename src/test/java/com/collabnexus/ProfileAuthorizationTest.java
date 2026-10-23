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
class ProfileAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void brandAndCreatorCannotUseEachOthersProfileEndpoints() throws Exception {
        MockHttpSession brand = register("brand", "Northwind Atelier");
        MockHttpSession creator = register("creator", null);

        mockMvc.perform(get("/api/brand/profile").session(brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Northwind Atelier"));

        mockMvc.perform(get("/api/creator/profile").session(brand))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(get("/api/brand/profile").session(creator))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/creator/profile").session(creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Demo Creator"));
    }

    @Test
    void brandCanUpdateItsOwnProfileAndTheAccountName() throws Exception {
        MockHttpSession brand = register("brand-update", "Northwind Atelier");

        mockMvc.perform(put("/api/brand/profile")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Northwind Atelier Group",
                                "companyWebsite", "https://example.com/group",
                                "industry", "Fashion & Lifestyle",
                                "country", "United Kingdom",
                                "description", "Premium lifestyle campaigns.",
                                "contactName", "Alex Morgan"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Northwind Atelier Group"))
                .andExpect(jsonPath("$.data.website").value("https://example.com/group"));

        mockMvc.perform(get("/api/auth/me").session(brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.fullName").value("Alex Morgan"))
                .andExpect(jsonPath("$.data.brand.companyName").value("Northwind Atelier Group"));
    }

    @Test
    void creatorCanUpdateItsOwnProfileAndRates() throws Exception {
        MockHttpSession creator = register("creator-update", null);

        mockMvc.perform(put("/api/creator/profile")
                        .session(creator)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("displayName", "Maya Chen"),
                                Map.entry("headline", "Beauty and skincare storyteller"),
                                Map.entry("bio", "Editorial beauty content for premium brands."),
                                Map.entry("niche", "Beauty & Skincare"),
                                Map.entry("primaryPlatform", "Instagram"),
                                Map.entry("country", "Singapore"),
                                Map.entry("languages", "English, Mandarin"),
                                Map.entry("contentFormats", "Short-form video, Photo carousel"),
                                Map.entry("baseRate", 1200.00),
                                Map.entry("currency", "usd"),
                                Map.entry("availability", "AVAILABLE"),
                                Map.entry("portfolioUrl", "https://example.com/maya")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Maya Chen"))
                .andExpect(jsonPath("$.data.baseRate").value(1200.00))
                .andExpect(jsonPath("$.data.currency").value("USD"));
    }

    @Test
    void invalidProfileUrlsAreRejected() throws Exception {
        MockHttpSession brand = register("brand-url", "Northwind Atelier");

        mockMvc.perform(put("/api/brand/profile")
                        .session(brand)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Northwind Atelier",
                                "companyWebsite", "javascript:alert(1)",
                                "contactName", "Jordan Ellis"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_URL"));
    }

    @Test
    void publicCreatorPreviewNeverExposesAccountEmail() throws Exception {
        MockHttpSession creator = register("privacy", null);
        MvcResult me = mockMvc.perform(get("/api/auth/me").session(creator))
                .andExpect(status().isOk())
                .andReturn();
        long profileId = objectMapper.readTree(me.getResponse().getContentAsString())
                .path("data").path("creator").path("id").asLong();

        mockMvc.perform(get("/api/creators/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Demo Creator"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist());
    }

    @Test
    void oneCreatorCannotReadAnotherCreatorsPrivateProfileThroughTheirSession() throws Exception {
        MockHttpSession first = register("creator-a", null);
        MockHttpSession second = register("creator-b", null);

        mockMvc.perform(put("/api/creator/profile")
                        .session(second)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "Second Creator",
                                "niche", "Travel & Hospitality",
                                "primaryPlatform", "YouTube",
                                "availability", "LIMITED"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/creator/profile").session(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Demo Creator"));

        mockMvc.perform(get("/api/creator/profile").session(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Second Creator"));
    }

    private MockHttpSession register(String prefix, String companyName) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        boolean brand = companyName != null;
        Map<String, Object> body = brand
                ? Map.of("fullName", "Jordan Ellis", "email", email, "password", "Demo!Pass1234",
                "role", "brand", "companyName", companyName, "companyWebsite", "https://example.com",
                "country", "United Kingdom")
                : Map.of("fullName", "Demo Creator", "email", email, "password", "Demo!Pass1234",
                "role", "creator", "primaryNiche", "Beauty & Skincare", "primaryPlatform", "Instagram",
                "country", "Singapore");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
