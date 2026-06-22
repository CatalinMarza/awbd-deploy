package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.GalleryReviewRequest;
import com.artgallery.dto.GalleryReviewResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Business logic for GalleryReview.
 *
 * A review must reference one visitor, one exhibition and one artwork.
 * The selected artwork must belong to the selected exhibition.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GalleryReviewService {

    private final GalleryReviewRepository reviewRepository;
    private final VisitorRepository visitorRepository;
    private final ArtworkRepository artworkRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse list(Pageable pageable) {
        log.debug("Listing reviews {}", pageable);
        return PageResponse.from(reviewRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse listByArtwork(Long artworkId, Pageable pageable) {
        return PageResponse.from(reviewRepository.findByArtworkId(artworkId, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse listByExhibition(Long exhibitionId, Pageable pageable) {
        return PageResponse.from(reviewRepository.findByExhibitionId(exhibitionId, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public GalleryReviewResponse get(Long id) {
        return mapper.toResponse(findReview(id));
    }

    public GalleryReviewResponse create(GalleryReviewRequest request) {
        GalleryReview review = new GalleryReview();

        apply(review, request);
        review.setReviewDate(request.reviewDate() != null ? request.reviewDate() : LocalDate.now());

        GalleryReview saved = reviewRepository.save(review);

        log.info(
                "Created review id={} visitorId={} exhibitionId={} artworkId={}",
                saved.getId(),
                request.visitorId(),
                request.exhibitionId(),
                request.artworkId()
        );

        return mapper.toResponse(saved);
    }

    public GalleryReviewResponse update(Long id, GalleryReviewRequest request) {
        GalleryReview review = findReview(id);

        apply(review, request);

        if (request.reviewDate() != null) {
            review.setReviewDate(request.reviewDate());
        } else if (review.getReviewDate() == null) {
            review.setReviewDate(LocalDate.now());
        }

        GalleryReview saved = reviewRepository.save(review);

        log.info(
                "Updated review id={} visitorId={} exhibitionId={} artworkId={}",
                id,
                request.visitorId(),
                request.exhibitionId(),
                request.artworkId()
        );

        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        GalleryReview review = findReview(id);

        reviewRepository.delete(review);

        log.info("Deleted review id={}", id);
    }

    private GalleryReview findReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GalleryReview", id));
    }

    private void apply(GalleryReview review, GalleryReviewRequest request) {
        Visitor visitor = visitorRepository.findById(request.visitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", request.visitorId()));

        Exhibition exhibition = exhibitionRepository.findById(request.exhibitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Exhibition", request.exhibitionId()));

        Artwork artwork = artworkRepository.findById(request.artworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", request.artworkId()));

        validateArtworkBelongsToExhibition(request.exhibitionId(), request.artworkId());

        review.setRating(request.rating());
        review.setReviewText(request.reviewText());
        review.setVisitor(visitor);
        review.setExhibition(exhibition);
        review.setArtwork(artwork);
    }

    private void validateArtworkBelongsToExhibition(Long exhibitionId, Long artworkId) {
        boolean artworkBelongsToExhibition =
                exhibitionRepository.existsArtworkInExhibition(exhibitionId, artworkId);

        if (!artworkBelongsToExhibition) {
            throw new BadRequestException(
                    "Selected artwork is not assigned to the selected exhibition."
            );
        }
    }
}
