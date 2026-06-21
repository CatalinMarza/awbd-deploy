package com.artgallery.dto;

import java.time.LocalDate;

/**
 * Read model for a {@code GalleryReview}.
 */
public record GalleryReviewResponse(
        Long id,
        Long visitorId,
        String visitorName,
        Long artworkId,
        String artworkTitle,
        Long exhibitionId,
        String exhibitionTitle,
        Integer rating,
        String reviewText,
        LocalDate reviewDate) {
}
