package com.artgallery.service;

import com.artgallery.dto.ExhibitionRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artwork;
import com.artgallery.model.Exhibition;
import com.artgallery.model.Exhibitor;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.ExhibitionRepository;
import com.artgallery.repository.ExhibitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExhibitionService (@ManyToMany)")
class ExhibitionServiceTest {

    @Mock
    private ExhibitionRepository exhibitionRepository;
    @Mock
    private ExhibitorRepository exhibitorRepository;
    @Mock
    private ArtworkRepository artworkRepository;

    private ExhibitionService service;

    @BeforeEach
    void setUp() {
        service = new ExhibitionService(exhibitionRepository, exhibitorRepository, artworkRepository, new DtoMapper());
    }

    private Exhibitor exhibitor() {
        return Exhibitor.builder().id(2L).name("Louvre").build();
    }

    private Exhibition exhibition(long id) {
        return Exhibition.builder().id(id).title("Expo").startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 1)).exhibitor(exhibitor()).build();
    }

    private ExhibitionRequest request(LocalDate start, LocalDate end) {
        return new ExhibitionRequest("Renaissance", start, end, 2L, "A show");
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(exhibitionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(exhibition(1)), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search() delegates to the repository")
    void search() {
        Pageable pageable = PageRequest.of(0, 10);
        when(exhibitionRepository.findByTitleContainingIgnoreCase("Exp", pageable))
                .thenReturn(new PageImpl<>(List.of(exhibition(1)), pageable, 1));
        assertThat(service.search("Exp", pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("create() succeeds with a valid date range")
    void createValid() {
        when(exhibitorRepository.findById(2L)).thenReturn(Optional.of(exhibitor()));
        when(exhibitionRepository.save(any(Exhibition.class))).thenAnswer(inv -> {
            Exhibition e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });
        assertThat(service.create(request(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1))).id()).isEqualTo(7L);
    }

    @Test
    @DisplayName("create() rejects an end date before the start date")
    void createInvalidDates() {
        assertThatThrownBy(() -> service.create(request(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1))))
                .isInstanceOf(BadRequestException.class);
        verify(exhibitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() fails when the exhibitor is missing")
    void createMissingExhibitor() {
        when(exhibitorRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        when(exhibitionRepository.findById(1L)).thenReturn(Optional.of(exhibition(1)));
        when(exhibitorRepository.findById(2L)).thenReturn(Optional.of(exhibitor()));
        when(exhibitionRepository.save(any(Exhibition.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(1L, request(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1))).title())
                .isEqualTo("Renaissance");
    }

    @Test
    @DisplayName("delete() removes the exhibition")
    void delete() {
        Exhibition existing = exhibition(1);
        when(exhibitionRepository.findById(1L)).thenReturn(Optional.of(existing));
        service.delete(1L);
        verify(exhibitionRepository).delete(existing);
    }

    @Test
    @DisplayName("listArtworks() maps the associated artworks")
    void listArtworks() {
        Exhibition exhibition = exhibition(1);
        Artwork artwork = Artwork.builder().id(10L).title("Mona Lisa")
                .artist(com.artgallery.model.Artist.builder().id(1L).name("Da Vinci").build()).build();
        exhibition.setArtworks(new ArrayList<>(List.of(artwork)));
        when(exhibitionRepository.findById(1L)).thenReturn(Optional.of(exhibition));

        var result = service.listArtworks(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Mona Lisa");
    }

    @Test
    @DisplayName("addArtwork() links the artwork to the exhibition")
    void addArtwork() {
        Exhibition exhibition = exhibition(1);
        Artwork artwork = Artwork.builder().id(10L).title("X").exhibitions(new ArrayList<>()).build();
        when(exhibitionRepository.findById(1L)).thenReturn(Optional.of(exhibition));
        when(artworkRepository.findById(10L)).thenReturn(Optional.of(artwork));

        service.addArtwork(1L, 10L);

        assertThat(artwork.getExhibitions()).contains(exhibition);
        verify(artworkRepository).save(artwork);
    }

    @Test
    @DisplayName("removeArtwork() unlinks the artwork from the exhibition")
    void removeArtwork() {
        Exhibition exhibition = exhibition(1);
        Artwork artwork = Artwork.builder().id(10L).title("X")
                .exhibitions(new ArrayList<>(List.of(exhibition))).build();
        when(exhibitionRepository.findById(1L)).thenReturn(Optional.of(exhibition));
        when(artworkRepository.findById(10L)).thenReturn(Optional.of(artwork));

        service.removeArtwork(1L, 10L);

        assertThat(artwork.getExhibitions()).doesNotContain(exhibition);
        verify(artworkRepository).save(artwork);
    }
}
