package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.GalleryReviewRequest;
import com.artgallery.dto.GalleryReviewResponse;
import com.artgallery.service.GalleryReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for gallery reviews.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class GalleryReviewController {

    private final GalleryReviewService service;

    @GetMapping
    public ApiResponse<PageResponse<GalleryReviewResponse>> list(
            @PageableDefault(size = 10, sort = "reviewDate") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/by-artwork/{artworkId}")
    public ApiResponse<PageResponse<GalleryReviewResponse>> byArtwork(
            @PathVariable Long artworkId,
            @PageableDefault(size = 10, sort = "reviewDate") Pageable pageable) {
        return ApiResponse.ok(service.listByArtwork(artworkId, pageable));
    }

    @GetMapping("/by-exhibition/{exhibitionId}")
    public ApiResponse<PageResponse<GalleryReviewResponse>> byExhibition(
            @PathVariable Long exhibitionId,
            @PageableDefault(size = 10, sort = "reviewDate") Pageable pageable) {
        return ApiResponse.ok(service.listByExhibition(exhibitionId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<GalleryReviewResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<GalleryReviewResponse>> create(@Valid @RequestBody GalleryReviewRequest request) {
        GalleryReviewResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Review created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GalleryReviewResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody GalleryReviewRequest request) {
        return ApiResponse.ok(service.update(id, request), "Review updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Review deleted successfully");
    }
}
