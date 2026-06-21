package com.artgallery.dto;

import java.time.LocalDate;

/**
 * Read model for a {@code Loan}.
 */
public record LoanResponse(
        Long id,
        Long artworkId,
        String artworkTitle,
        Long exhibitorId,
        String exhibitorName,
        LocalDate startDate,
        LocalDate endDate,
        String conditions) {
}
