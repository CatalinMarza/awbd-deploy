package com.artgallery.repository;

import com.artgallery.model.GalleryReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryReviewRepository extends JpaRepository<GalleryReview, Long> {

    @Override
    @EntityGraph(attributePaths = {"visitor", "artwork", "exhibition"})
    Page<GalleryReview> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"visitor", "artwork", "exhibition"})
    Page<GalleryReview> findByArtworkId(Long artworkId, Pageable pageable);

    @EntityGraph(attributePaths = {"visitor", "artwork", "exhibition"})
    Page<GalleryReview> findByExhibitionId(Long exhibitionId, Pageable pageable);

    @EntityGraph(attributePaths = {"visitor", "artwork", "exhibition"})
    Page<GalleryReview> findByVisitorId(Long visitorId, Pageable pageable);
}
