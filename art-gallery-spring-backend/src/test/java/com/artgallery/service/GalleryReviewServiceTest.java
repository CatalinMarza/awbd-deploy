package com.artgallery.service;

import com.artgallery.dto.GalleryReviewRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artwork;
import com.artgallery.model.Exhibition;
import com.artgallery.model.GalleryReview;
import com.artgallery.model.Visitor;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.ExhibitionRepository;
import com.artgallery.repository.GalleryReviewRepository;
import com.artgallery.repository.VisitorRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GalleryReviewService (artwork and/or exhibition rule)")
class GalleryReviewServiceTest {

    @Mock
    private GalleryReviewRepository reviewRepository;
    @Mock
    private VisitorRepository visitorRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private ExhibitionRepository exhibitionRepository;

    private GalleryReviewService service;

    @BeforeEach
    void setUp() {
        service = new GalleryReviewService(reviewRepository, visitorRepository,
                artworkRepository, exhibitionRepository, new DtoMapper());
    }

    private void stubVisitor() {
        lenient().when(visitorRepository.findById(1L))
                .thenReturn(Optional.of(Visitor.builder().id(1L).name("Ana").build()));
    }

    @Test
    @DisplayName("create() succeeds when an artwork is referenced")
    void createWithArtwork() {
        stubVisitor();
        when(artworkRepository.findById(2L))
                .thenReturn(Optional.of(Artwork.builder().id(2L).title("X").build()));
        when(reviewRepository.save(any(GalleryReview.class))).thenAnswer(inv -> {
            GalleryReview r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });
        GalleryReviewRequest request = new GalleryReviewRequest(
                1L, 2L, null, 5, "Wonderful", LocalDate.of(2024, 3, 1));

        var response = service.create(request);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.artworkId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("create() succeeds when only an exhibition is referenced")
    void createWithExhibition() {
        stubVisitor();
        when(exhibitionRepository.findById(3L))
                .thenReturn(Optional.of(Exhibition.builder().id(3L).title("Expo").build()));
        when(reviewRepository.save(any(GalleryReview.class))).thenAnswer(inv -> inv.getArgument(0));
        GalleryReviewRequest request = new GalleryReviewRequest(
                1L, null, 3L, 4, null, LocalDate.of(2024, 3, 1));

        var response = service.create(request);

        assertThat(response.exhibitionId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("create() rejects a review with neither artwork nor exhibition")
    void createWithoutTargetThrows() {
        GalleryReviewRequest request = new GalleryReviewRequest(
                1L, null, null, 5, "Orphan", LocalDate.of(2024, 3, 1));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("artwork and/or an exhibition");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() fails when the visitor does not exist")
    void createMissingVisitor() {
        when(visitorRepository.findById(1L)).thenReturn(Optional.empty());
        GalleryReviewRequest request = new GalleryReviewRequest(
                1L, 2L, null, 5, null, LocalDate.of(2024, 3, 1));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Visitor");
    }

    private GalleryReview sampleReview(long id) {
        return GalleryReview.builder().id(id)
                .visitor(Visitor.builder().id(1L).name("Ana").build())
                .artwork(Artwork.builder().id(2L).title("X").build())
                .rating(5).reviewText("ok").reviewDate(LocalDate.of(2024, 3, 1)).build();
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleReview(1)), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByArtwork() delegates to the repository")
    void listByArtwork() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByArtworkId(2L, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleReview(1)), pageable, 1));
        assertThat(service.listByArtwork(2L, pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("listByExhibition() delegates to the repository")
    void listByExhibition() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByExhibitionId(3L, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleReview(1)), pageable, 1));
        assertThat(service.listByExhibition(3L, pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(sampleReview(5)));
        when(visitorRepository.findById(1L)).thenReturn(Optional.of(Visitor.builder().id(1L).name("Ana").build()));
        when(artworkRepository.findById(2L)).thenReturn(Optional.of(Artwork.builder().id(2L).title("X").build()));
        when(reviewRepository.save(any(GalleryReview.class))).thenAnswer(inv -> inv.getArgument(0));
        service.update(5L, new GalleryReviewRequest(1L, 2L, null, 4, "good", LocalDate.of(2024, 3, 2)));
        verify(reviewRepository).save(any(GalleryReview.class));
    }

    @Test
    @DisplayName("delete() removes the review")
    void delete() {
        GalleryReview existing = sampleReview(5);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(existing));
        service.delete(5L);
        verify(reviewRepository).delete(existing);
    }
}
