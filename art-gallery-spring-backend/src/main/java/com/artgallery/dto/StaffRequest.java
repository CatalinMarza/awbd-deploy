package com.artgallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating/updating a {@code Staff} member.
 */
public record StaffRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 128, message = "Name must be at most 128 characters")
        String name,

        @NotBlank(message = "Role is required")
        @Size(max = 64, message = "Role must be at most 64 characters")
        String role,

        @NotNull(message = "Hire date is required")
        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate,

        @Size(max = 32, message = "Certification level must be at most 32 characters")
        String certificationLevel) {
}
