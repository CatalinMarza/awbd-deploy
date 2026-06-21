package com.artgallery.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read model for an {@code InsurancePolicy}.
 */
public record InsurancePolicyResponse(
        Long id,
        String provider,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalCoverageAmount) {
}
