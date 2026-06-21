package com.artgallery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Payload for creating/updating an {@code Insurance} record.
 */
public record InsuranceRequest(

        @NotNull(message = "Artwork is required")
        Long artworkId,

        @NotNull(message = "Policy is required")
        Long policyId,

        @NotNull(message = "Insured amount is required")
        @Positive(message = "Insured amount must be greater than zero")
        BigDecimal insuredAmount) {
}
