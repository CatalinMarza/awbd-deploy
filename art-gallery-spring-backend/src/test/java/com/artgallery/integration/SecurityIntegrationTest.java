package com.artgallery.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.artgallery.dto.ArtistRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security integration")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("anonymous access to a protected endpoint returns 401 JSON")
    void anonymousReturns401() throws Exception {
        mockMvc.perform(get("/api/artists"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("login with the seeded admin account authenticates the session")
    void adminLoginSucceeds() throws Exception {
        mockMvc.perform(formLogin("/api/auth/login").user("admin").password("admin123"))
                .andExpect(authenticated().withRoles("ADMIN", "USER"));
    }

    @Test
    @DisplayName("login with a wrong password is rejected")
    void wrongPasswordFails() throws Exception {
        mockMvc.perform(formLogin("/api/auth/login").user("admin").password("nope"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    @DisplayName("ADMIN can create an artist (201)")
    void adminCanCreate() throws Exception {
        ArtistRequest request = new ArtistRequest("Security Test Artist", "Italian", 1850, 1920);
        mockMvc.perform(post("/api/artists").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Security Test Artist"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("USER is forbidden from creating an artist (403)")
    void userCannotCreate() throws Exception {
        ArtistRequest request = new ArtistRequest("Forbidden Artist", null, null, null);
        mockMvc.perform(post("/api/artists").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
