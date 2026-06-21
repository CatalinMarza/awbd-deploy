package com.artgallery.dto;

/**
 * Read model for an {@code Artist}.
 */
public record ArtistResponse(
        Long id,
        String name,
        String nationality,
        Integer birthYear,
        Integer deathYear) {
}
