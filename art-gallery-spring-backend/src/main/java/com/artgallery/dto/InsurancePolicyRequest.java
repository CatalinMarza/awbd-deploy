package com.artgallery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating/updating an {@code InsurancePolicy}.
 * The rule {@code endDate >= startDate} is enforced in the service layer.
 */
public record InsurancePolicyRequest(

        @NotBlank(message = "Provider is required")
        String provider,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @PositiveOrZero(message = "Total coverage amount must be zero or greater")
        BigDecimal totalCoverageAmount) {
}
