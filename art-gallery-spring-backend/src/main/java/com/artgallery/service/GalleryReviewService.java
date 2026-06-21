package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.GalleryReviewRequest;
import com.artgallery.dto.GalleryReviewResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.GalleryReview;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.ExhibitionRepository;
import com.artgallery.repository.GalleryReviewRepository;
import com.artgallery.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link GalleryReview}. A review must reference an artwork
 * and/or an exhibition.
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
    public PageResponse<GalleryReviewResponse> list(Pageable pageable) {
        log.debug("Listing reviews {}", pageable);
        return PageResponse.from(reviewRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<GalleryReviewResponse> listByArtwork(Long artworkId, Pageable pageable) {
        return PageResponse.from(reviewRepository.findByArtworkId(artworkId, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<GalleryReviewResponse> listByExhibition(Long exhibitionId, Pageable pageable) {
        return PageResponse.from(reviewRepository.findByExhibitionId(exhibitionId, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public GalleryReviewResponse get(Long id) {
        return mapper.toResponse(findReview(id));
    }

    public GalleryReviewResponse create(GalleryReviewRequest request) {
        GalleryReview review = new GalleryReview();
        apply(review, request);
        GalleryReview saved = reviewRepository.save(review);
        log.info("Created review id={} visitorId={}", saved.getId(), request.visitorId());
        return mapper.toResponse(saved);
    }

    public GalleryReviewResponse update(Long id, GalleryReviewRequest request) {
        GalleryReview review = findReview(id);
        apply(review, request);
        GalleryReview saved = reviewRepository.save(review);
        log.info("Updated review id={}", id);
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

    private void apply(GalleryReview review, GalleryReviewRequest r) {
        if (r.artworkId() == null && r.exhibitionId() == null) {
            throw new BadRequestException("A review must reference an artwork and/or an exhibition");
        }
        review.setRating(r.rating());
        review.setReviewText(r.reviewText());
        review.setReviewDate(r.reviewDate());
        review.setVisitor(visitorRepository.findById(r.visitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", r.visitorId())));
        review.setArtwork(r.artworkId() == null ? null
                : artworkRepository.findById(r.artworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", r.artworkId())));
        review.setExhibition(r.exhibitionId() == null ? null
                : exhibitionRepository.findById(r.exhibitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Exhibition", r.exhibitionId())));
    }
}
