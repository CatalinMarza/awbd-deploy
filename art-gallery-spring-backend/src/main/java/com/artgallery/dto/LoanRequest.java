package com.artgallery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating/updating a {@code Loan}.
 * The rule {@code endDate >= startDate} is enforced in the service layer.
 */
public record LoanRequest(

        @NotNull(message = "Artwork is required")
        Long artworkId,

        @NotNull(message = "Exhibitor is required")
        Long exhibitorId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 512, message = "Conditions must be at most 512 characters")
        String conditions) {
}
