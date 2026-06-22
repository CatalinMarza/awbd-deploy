package com.artgallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating/updating a GalleryReview.
 *
 * The frontend no longer displays the review date.
 * The field is kept for backward compatibility with existing tests/API calls.
 * If it is missing, the service generates/preserves the date.
 */
public record GalleryReviewRequest(
        @NotNull(message = "Visitor is required")
        Long visitorId,

        @NotNull(message = "Artwork is required")
        Long artworkId,

        @NotNull(message = "Exhibition is required")
        Long exhibitionId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @Size(max = 256, message = "Review text must be at most 256 characters")
        String reviewText,

        LocalDate reviewDate
) {
}
