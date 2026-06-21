package com.artgallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating/updating an {@code Artist}.
 */
public record ArtistRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 128, message = "Name must be at most 128 characters")
        String name,

        @Size(max = 64, message = "Nationality must be at most 64 characters")
        String nationality,

        @Min(value = 0, message = "Birth year must be a positive number")
        @Max(value = 2100, message = "Birth year is not realistic")
        Integer birthYear,

        @Min(value = 0, message = "Death year must be a positive number")
        @Max(value = 2100, message = "Death year is not realistic")
        Integer deathYear) {
}
