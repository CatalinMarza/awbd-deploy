package com.artgallery.dto;

import java.time.LocalDate;

/**
 * Read model for a {@code Collection}.
 */
public record CollectionResponse(
        Long id,
        String name,
        String description,
        LocalDate createdDate) {
}
