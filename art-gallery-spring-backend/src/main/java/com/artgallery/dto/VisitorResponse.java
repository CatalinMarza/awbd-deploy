package com.artgallery.dto;

import java.time.LocalDate;

/**
 * Read model for a {@code Visitor}.
 */
public record VisitorResponse(
        Long id,
        String name,
        String email,
        String phone,
        String membershipType,
        LocalDate joinDate) {
}
