package com.artgallery.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating/updating a Visitor.
 *
 * The frontend no longer displays the join date.
 * The field is kept for backward compatibility with existing tests/API calls.
 * If it is missing, the service generates/preserves the date.
 */
public record VisitorRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 128, message = "Name must be at most 128 characters")
        String name,

        @Email(message = "Email must be a valid address")
        @Size(max = 128, message = "Email must be at most 128 characters")
        String email,

        @Size(max = 32, message = "Phone must be at most 32 characters")
        String phone,

        @Size(max = 32, message = "Membership type must be at most 32 characters")
        String membershipType,

        @PastOrPresent(message = "Join date cannot be in the future")
        LocalDate joinDate
) {
}
