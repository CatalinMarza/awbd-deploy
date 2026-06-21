package com.artgallery.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read model for an {@code Acquisition}.
 */
public record AcquisitionResponse(
        Long id,
        Long artworkId,
        String artworkTitle,
        LocalDate acquisitionDate,
        String acquisitionType,
        BigDecimal price,
        Long staffId,
        String staffName) {
}
