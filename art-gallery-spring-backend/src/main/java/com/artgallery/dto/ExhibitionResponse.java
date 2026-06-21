package com.artgallery.dto;

import java.time.LocalDate;

/**
 * Read model for an {@code Exhibition}.
 */
public record ExhibitionResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Long exhibitorId,
        String exhibitorName,
        String description) {
}
