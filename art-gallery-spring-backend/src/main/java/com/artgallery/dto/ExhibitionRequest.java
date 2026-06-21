package com.artgallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating/updating an {@code Exhibition}.
 */
public record ExhibitionRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 128, message = "Title must be at most 128 characters")
        String title,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Exhibitor is required")
        Long exhibitorId,

        @Size(max = 512, message = "Description must be at most 512 characters")
        String description) {
}
