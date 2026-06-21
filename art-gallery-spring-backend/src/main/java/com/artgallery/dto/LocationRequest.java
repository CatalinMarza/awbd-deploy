package com.artgallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating/updating a {@code Location}.
 */
public record LocationRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 128, message = "Name must be at most 128 characters")
        String name,

        @Size(max = 32, message = "Gallery room must be at most 32 characters")
        String galleryRoom,

        @Size(max = 32, message = "Type must be at most 32 characters")
        String type,

        @Positive(message = "Capacity must be greater than zero")
        Integer capacity) {
}
