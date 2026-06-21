package com.artgallery.dto;

import java.time.LocalDate;

/**
 * Read model for a {@code Staff} member.
 */
public record StaffResponse(
        Long id,
        String name,
        String role,
        LocalDate hireDate,
        String certificationLevel) {
}
