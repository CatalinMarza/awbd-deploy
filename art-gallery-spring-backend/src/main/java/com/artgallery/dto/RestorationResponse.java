package com.artgallery.dto;

import java.time.LocalDate;

/**
 * Read model for a {@code Restoration}.
 */
public record RestorationResponse(
        Long id,
        Long artworkId,
        String artworkTitle,
        Long staffId,
        String staffName,
        LocalDate startDate,
        LocalDate endDate,
        String description) {
}
