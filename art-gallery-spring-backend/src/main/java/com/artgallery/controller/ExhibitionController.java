package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.ArtworkResponse;
import com.artgallery.dto.ExhibitionRequest;
import com.artgallery.dto.ExhibitionResponse;
import com.artgallery.service.ExhibitionService;
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

import java.util.List;

/**
 * REST endpoints for exhibitions, including management of the
 * {@code @ManyToMany} association with artworks.
 */
@RestController
@RequestMapping("/api/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService service;

    @GetMapping
    public ApiResponse<PageResponse<ExhibitionResponse>> list(
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ExhibitionResponse>> search(
            @RequestParam("q") String q,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ApiResponse.ok(service.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExhibitionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/{id}/artworks")
    public ApiResponse<List<ArtworkResponse>> artworks(@PathVariable Long id) {
        return ApiResponse.ok(service.listArtworks(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExhibitionResponse>> create(@Valid @RequestBody ExhibitionRequest request) {
        ExhibitionResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Exhibition created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ExhibitionResponse> update(@PathVariable Long id, @Valid @RequestBody ExhibitionRequest request) {
        return ApiResponse.ok(service.update(id, request), "Exhibition updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Exhibition deleted successfully");
    }

    @PostMapping("/{id}/artworks/{artworkId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> addArtwork(@PathVariable Long id, @PathVariable Long artworkId) {
        service.addArtwork(id, artworkId);
        return ApiResponse.<Void>ok(null, "Artwork added to exhibition");
    }

    @DeleteMapping("/{id}/artworks/{artworkId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> removeArtwork(@PathVariable Long id, @PathVariable Long artworkId) {
        service.removeArtwork(id, artworkId);
        return ApiResponse.<Void>ok(null, "Artwork removed from exhibition");
    }
}
