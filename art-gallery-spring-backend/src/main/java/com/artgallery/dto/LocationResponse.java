package com.artgallery.dto;

/**
 * Read model for a {@code Location}.
 */
public record LocationResponse(
        Long id,
        String name,
        String galleryRoom,
        String type,
        Integer capacity) {
}
