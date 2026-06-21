package com.artgallery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating/updating a {@code Restoration}.
 * The rule {@code endDate >= startDate} is enforced in the service layer.
 */
public record RestorationRequest(

        @NotNull(message = "Artwork is required")
        Long artworkId,

        @NotNull(message = "Staff member is required")
        Long staffId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 512, message = "Description must be at most 512 characters")
        String description) {
}
