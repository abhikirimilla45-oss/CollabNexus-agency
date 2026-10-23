package com.collabnexus;

import com.collabnexus.users.AccountStatus;
import com.collabnexus.users.User;
import com.collabnexus.users.UserRepository;
import jakarta.servlet.http.HttpSession;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void brandRegistrationCreatesSessionAndProfile() throws Exception {
        String email = email("brand");
        MvcResult result = register(email, "brand", "Northwind Atelier");

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value("BRAND"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.brand.companyName").value("Northwind Atelier"))
                .andExpect(jsonPath("$.data.creator").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    void creatorRegistrationCreatesCreatorProfile() throws Exception {
        String email = email("creator");
        MvcResult result = register(email, "creator", null);

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("CREATOR"))
                .andExpect(jsonPath("$.data.creator.displayName").value("Demo Creator"))
                .andExpect(jsonPath("$.data.creator.niche").value("Beauty & Skincare"))
                .andExpect(jsonPath("$.data.brand").doesNotExist());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = email("duplicate");
        register(email, "creator", null);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "creator", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("EMAIL_IN_USE"));
    }

    @Test
    void weakPasswordAndAdminSelfRegistrationAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapperJson(Map.of(
                                "fullName", "Weak User",
                                "email", email("weak"),
                                "password", "password",
                                "role", "creator",
                                "primaryNiche", "Travel & Hospitality",
                                "primaryPlatform", "Instagram"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapperJson(Map.of(
                                "fullName", "Sneaky Admin",
                                "email", email("admin"),
                                "password", "Strong!Pass123",
                                "role", "ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ROLE"));
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUser() throws Exception {
        String email = email("login");
        register(email, "creator", null);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapperJson(Map.of("email", email, "password", "Wrong!Pass123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapperJson(Map.of("email", "missing@example.com", "password", "Wrong!Pass123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suspendedAccountCannotLogInAndExistingSessionIsInvalidated() throws Exception {
        String email = email("suspended");
        MvcResult result = register(email, "creator", null);
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setStatus(AccountStatus.SUSPENDED);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SUSPENDED"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapperJson(Map.of("email", email, "password", "Demo!Pass1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        MvcResult result = register(email("logout"), "creator", null);
        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/auth/logout").session((MockHttpSession) session).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").session((MockHttpSession) session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousRequestsAreRejectedWithJson() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private MvcResult register(String email, String role, String companyName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, role, companyName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return result;
    }

    private String registerBody(String email, String role, String companyName) {
        Map<String, String> body = role.equals("brand")
                ? Map.of("fullName", "Jordan Ellis", "email", email, "password", "Demo!Pass1234",
                "role", role, "companyName", companyName, "companyWebsite", "https://example.com",
                "country", "United Kingdom")
                : Map.of("fullName", "Demo Creator", "email", email, "password", "Demo!Pass1234",
                "role", role, "primaryNiche", "Beauty & Skincare", "primaryPlatform", "Instagram",
                "country", "Singapore");
        return objectMapperJson(body);
    }

    private String email(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String objectMapperJson(Map<String, String> body) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
