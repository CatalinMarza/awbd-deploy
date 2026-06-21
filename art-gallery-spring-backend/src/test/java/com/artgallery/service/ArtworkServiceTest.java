package com.artgallery.service;

import com.artgallery.dto.ArtworkRequest;
import com.artgallery.dto.ArtworkResponse;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artist;
import com.artgallery.model.Artwork;
import com.artgallery.model.Collection;
import com.artgallery.repository.ArtistRepository;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.CollectionRepository;
import com.artgallery.repository.LocationRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtworkService")
class ArtworkServiceTest {

    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private LocationRepository locationRepository;

    private ArtworkService service;

    @BeforeEach
    void setUp() {
        service = new ArtworkService(artworkRepository, artistRepository,
                collectionRepository, locationRepository, new DtoMapper());
    }

    private Artist artist() {
        return Artist.builder().id(1L).name("Frida").build();
    }

    @Test
    @DisplayName("create() resolves the artist and an optional collection")
    void createResolvesRelations() {
        ArtworkRequest request = new ArtworkRequest(
                "Self Portrait", 1L, 1940, "Oil", 2L, null, new BigDecimal("1000.00"));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist()));
        when(collectionRepository.findById(2L))
                .thenReturn(Optional.of(Collection.builder().id(2L).name("Modern").build()));
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(inv -> {
            Artwork a = inv.getArgument(0);
            a.setId(7L);
            return a;
        });

        ArtworkResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.artistName()).isEqualTo("Frida");
        assertThat(response.collectionName()).isEqualTo("Modern");

        ArgumentCaptor<Artwork> captor = ArgumentCaptor.forClass(Artwork.class);
        verify(artworkRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Self Portrait");
        assertThat(captor.getValue().getCollection()).isNotNull();
    }

    @Test
    @DisplayName("create() leaves collection/location null when not provided")
    void createWithoutOptionalRelations() {
        ArtworkRequest request = new ArtworkRequest("Untitled", 1L, null, null, null, null, null);
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist()));
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(inv -> inv.getArgument(0));

        ArtworkResponse response = service.create(request);

        assertThat(response.collectionId()).isNull();
        assertThat(response.locationId()).isNull();
        verify(collectionRepository, never()).findById(any());
        verify(locationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("create() fails when the referenced artist does not exist")
    void createMissingArtistThrows() {
        ArtworkRequest request = new ArtworkRequest("X", 99L, null, null, null, null, null);
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Artist");
        verify(artworkRepository, never()).save(any());
    }

    @Test
    @DisplayName("get() throws when the artwork is missing")
    void getMissingThrows() {
        when(artworkRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(50L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Artwork");
    }

    @Test
    @DisplayName("delete() removes an existing artwork")
    void deleteRemoves() {
        Artwork artwork = Artwork.builder().id(4L).title("X").artist(artist()).build();
        when(artworkRepository.findById(4L)).thenReturn(Optional.of(artwork));

        service.delete(4L);

        verify(artworkRepository).delete(artwork);
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        Artwork artwork = Artwork.builder().id(1L).title("Mona Lisa").artist(artist()).build();
        when(artworkRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(artwork), pageable, 1));

        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search() delegates to the repository query")
    void search() {
        Pageable pageable = PageRequest.of(0, 10);
        Artwork artwork = Artwork.builder().id(1L).title("Mona Lisa").artist(artist()).build();
        when(artworkRepository.search("Mona", pageable))
                .thenReturn(new PageImpl<>(List.of(artwork), pageable, 1));

        assertThat(service.search("Mona", pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("update() applies the changes onto an existing artwork")
    void update() {
        Artwork existing = Artwork.builder().id(2L).title("Old").artist(artist()).build();
        when(artworkRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist()));
        when(artworkRepository.save(any(Artwork.class))).thenAnswer(inv -> inv.getArgument(0));

        ArtworkResponse response = service.update(2L,
                new ArtworkRequest("New Title", 1L, 1500, "Tempera", null, null, null));

        assertThat(response.title()).isEqualTo("New Title");
    }
}
