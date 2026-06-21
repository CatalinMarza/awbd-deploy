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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
@DisplayName("Pagination & sorting integration")
class ArtistPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArtistRepository artistRepository;

    @BeforeEach
    void seed() throws Exception {
        artistRepository.deleteAll();
        for (String name : new String[]{"Charlie", "Alice", "Bob", "Eve", "Dave"}) {
            ArtistRequest request = new ArtistRequest(name, "Test", 1900, null);
            mockMvc.perform(post("/api/artists").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("first page is sized and sorted ascending by name")
    void firstPageSortedAsc() throws Exception {
        mockMvc.perform(get("/api/artists")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.data.content[1].name").value("Bob"));
    }

    @Test
    @DisplayName("second page continues the ascending order")
    void secondPage() throws Exception {
        mockMvc.perform(get("/api/artists")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.content[0].name").value("Charlie"))
                .andExpect(jsonPath("$.data.content[1].name").value("Dave"));
    }

    @Test
    @DisplayName("descending sort reverses the order")
    void descendingSort() throws Exception {
        mockMvc.perform(get("/api/artists")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Eve"));
    }
}
