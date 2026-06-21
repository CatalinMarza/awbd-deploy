package com.artgallery.dto;

import java.math.BigDecimal;

/**
 * Read model for an {@code Insurance} record.
 */
public record InsuranceResponse(
        Long id,
        Long artworkId,
        String artworkTitle,
        Long policyId,
        String policyProvider,
        BigDecimal insuredAmount) {
}
