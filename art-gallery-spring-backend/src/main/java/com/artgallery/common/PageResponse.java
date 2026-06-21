package com.artgallery.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Serializable view of a Spring Data {@link Page}, exposing the content together
 * with the pagination/sorting metadata used by the Vue.js frontend.
 *
 * @param <T> the element type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        int numberOfElements) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements());
    }
}
