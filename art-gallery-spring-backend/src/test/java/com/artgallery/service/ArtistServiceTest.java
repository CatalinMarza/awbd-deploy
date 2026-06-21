package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.ArtistRequest;
import com.artgallery.dto.ArtistResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artist;
import com.artgallery.repository.ArtistRepository;
import com.artgallery.repository.ArtworkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtistService")
class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private ArtworkRepository artworkRepository;

    private ArtistService service;

    @BeforeEach
    void setUp() {
        service = new ArtistService(artistRepository, artworkRepository, new DtoMapper());
    }

    private Artist artist(long id, String name) {
        return Artist.builder().id(id).name(name).nationality("Mexican").birthYear(1907).build();
    }

    @Test
    @DisplayName("list() maps the page into a PageResponse")
    void listMapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(artistRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(artist(1, "Frida"), artist(2, "Diego")), pageable, 2));

        PageResponse<ArtistResponse> result = service.list(pageable);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).extracting(ArtistResponse::name).containsExactly("Frida", "Diego");
    }

    @Test
    @DisplayName("get() returns the mapped artist when it exists")
    void getReturnsArtist() {
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist(1, "Frida")));

        ArtistResponse response = service.get(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Frida");
    }

    @Test
    @DisplayName("get() throws ResourceNotFoundException for a missing id")
    void getMissingThrows() {
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Artist")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create() persists a new artist when the name is unique")
    void createPersists() {
        ArtistRequest request = new ArtistRequest("Frida", "Mexican", 1907, 1954);
        when(artistRepository.existsByNameIgnoreCase("Frida")).thenReturn(false);
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> {
            Artist a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        ArtistResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(10L);
        ArgumentCaptor<Artist> captor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Frida");
        assertThat(captor.getValue().getDeathYear()).isEqualTo(1954);
    }

    @Test
    @DisplayName("create() rejects a duplicate name with DuplicateResourceException")
    void createDuplicateThrows() {
        ArtistRequest request = new ArtistRequest("Frida", "Mexican", 1907, 1954);
        when(artistRepository.existsByNameIgnoreCase("Frida")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Frida");
        verify(artistRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() applies the new values onto an existing artist")
    void updateApplies() {
        Artist existing = artist(5, "Old");
        when(artistRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));

        ArtistResponse response = service.update(5L, new ArtistRequest("New", "French", 1900, null));

        assertThat(response.name()).isEqualTo("New");
        assertThat(existing.getNationality()).isEqualTo("French");
    }

    @Test
    @DisplayName("delete() removes an artist with no artworks")
    void deleteSucceeds() {
        Artist existing = artist(3, "Frida");
        when(artistRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(artworkRepository.countByArtistId(3L)).thenReturn(0L);

        service.delete(3L);

        verify(artistRepository).delete(existing);
    }

    @Test
    @DisplayName("delete() is blocked when artworks still reference the artist")
    void deleteBlockedByReferences() {
        when(artistRepository.findById(3L)).thenReturn(Optional.of(artist(3, "Frida")));
        when(artworkRepository.countByArtistId(3L)).thenReturn(4L);

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("4 artwork");
        verify(artistRepository, never()).delete(any());
    }
}
