package com.artgallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating/updating a {@code GalleryReview}.
 * The rule "at least one of artwork/exhibition must be set" is enforced in the service.
 */
public record GalleryReviewRequest(

        @NotNull(message = "Visitor is required")
        Long visitorId,

        Long artworkId,

        Long exhibitionId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @Size(max = 256, message = "Review text must be at most 256 characters")
        String reviewText,

        @NotNull(message = "Review date is required")
        LocalDate reviewDate) {
}
