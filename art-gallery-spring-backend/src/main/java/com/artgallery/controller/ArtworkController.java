package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.ArtworkRequest;
import com.artgallery.dto.ArtworkResponse;
import com.artgallery.service.ArtworkService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for artworks.
 * Supports pagination &amp; sorting, e.g.
 * {@code GET /api/artworks?page=0&size=10&sort=title,asc&sort=yearCreated,desc}.
 */
@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService service;

    @GetMapping
    public ApiResponse<PageResponse<ArtworkResponse>> list(
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ArtworkResponse>> search(
            @RequestParam("q") String q,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ApiResponse.ok(service.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArtworkResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ArtworkResponse>> create(@Valid @RequestBody ArtworkRequest request) {
        ArtworkResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Artwork created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ArtworkResponse> update(@PathVariable Long id, @Valid @RequestBody ArtworkRequest request) {
        return ApiResponse.ok(service.update(id, request), "Artwork updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Artwork deleted successfully");
    }
}
