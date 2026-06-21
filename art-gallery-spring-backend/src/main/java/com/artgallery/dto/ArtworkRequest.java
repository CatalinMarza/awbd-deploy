package com.artgallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for creating/updating an {@code Artwork}.
 */
public record ArtworkRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 128, message = "Title must be at most 128 characters")
        String title,

        @NotNull(message = "Artist is required")
        Long artistId,

        @Min(value = 0, message = "Year created must be a positive number")
        @Max(value = 2100, message = "Year created is not realistic")
        Integer yearCreated,

        @Size(max = 64, message = "Medium must be at most 64 characters")
        String medium,

        Long collectionId,

        Long locationId,

        @PositiveOrZero(message = "Estimated value must be zero or greater")
        BigDecimal estimatedValue) {
}
