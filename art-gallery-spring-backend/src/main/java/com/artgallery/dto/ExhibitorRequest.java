package com.artgallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating/updating an {@code Exhibitor}.
 */
public record ExhibitorRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 128, message = "Name must be at most 128 characters")
        String name,

        @Size(max = 256, message = "Address must be at most 256 characters")
        String address,

        @Size(max = 64, message = "City must be at most 64 characters")
        String city,

        @Size(max = 256, message = "Contact info must be at most 256 characters")
        String contactInfo) {
}
