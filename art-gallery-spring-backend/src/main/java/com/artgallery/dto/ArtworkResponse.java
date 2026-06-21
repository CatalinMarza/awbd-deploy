package com.artgallery.dto;

import java.math.BigDecimal;

/**
 * Read model for an {@code Artwork}, including the display names of its relations.
 */
public record ArtworkResponse(
        Long id,
        String title,
        Long artistId,
        String artistName,
        Integer yearCreated,
        String medium,
        Long collectionId,
        String collectionName,
        Long locationId,
        String locationName,
        BigDecimal estimatedValue) {
}
