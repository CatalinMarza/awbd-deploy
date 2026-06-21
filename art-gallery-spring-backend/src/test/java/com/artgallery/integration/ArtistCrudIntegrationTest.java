package com.artgallery.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.artgallery.dto.ArtistRequest;
import com.artgallery.repository.ArtistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
@DisplayName("Artist CRUD integration")
class ArtistCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArtistRepository artistRepository;

    @BeforeEach
    void clean() {
        artistRepository.deleteAll();
    }

    private Long createArtist(String name) throws Exception {
        ArtistRequest request = new ArtistRequest(name, "Dutch", 1853, 1890);
        MvcResult result = mockMvc.perform(post("/api/artists").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("full create -> read -> delete round trip")
    void crudRoundTrip() throws Exception {
        Long id = createArtist("Vincent van Gogh");

        mockMvc.perform(get("/api/artists/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Vincent van Gogh"));

        mockMvc.perform(delete("/api/artists/{id}", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/artists/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("blank name is rejected with a 400 validation error")
    void validationError() throws Exception {
        ArtistRequest invalid = new ArtistRequest("", null, null, null);
        mockMvc.perform(post("/api/artists").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    @DisplayName("duplicate name is rejected with a 409 conflict")
    void duplicateName() throws Exception {
        createArtist("Claude Monet");

        ArtistRequest duplicate = new ArtistRequest("Claude Monet", "French", 1840, 1926);
        mockMvc.perform(post("/api/artists").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("requesting a missing artist returns 404")
    void missingReturns404() throws Exception {
        mockMvc.perform(get("/api/artists/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
