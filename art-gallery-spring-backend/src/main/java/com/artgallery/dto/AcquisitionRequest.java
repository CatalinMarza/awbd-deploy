package com.artgallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating/updating an {@code Acquisition} (the {@code @OneToOne}
 * record describing how an artwork entered the gallery).
 */
public record AcquisitionRequest(

        @NotNull(message = "Artwork is required")
        Long artworkId,

        @NotNull(message = "Acquisition date is required")
        LocalDate acquisitionDate,

        @NotBlank(message = "Acquisition type is required")
        @Size(max = 32, message = "Acquisition type must be at most 32 characters")
        String acquisitionType,

        @PositiveOrZero(message = "Price must be zero or greater")
        BigDecimal price,

        @NotNull(message = "Staff member is required")
        Long staffId) {
}
