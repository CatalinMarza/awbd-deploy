package com.artgallery.dto;

/**
 * Read model for an {@code Exhibitor}.
 */
public record ExhibitorResponse(
        Long id,
        String name,
        String address,
        String city,
        String contactInfo) {
}
