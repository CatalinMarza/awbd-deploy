package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.RestorationRequest;
import com.artgallery.dto.RestorationResponse;
import com.artgallery.service.RestorationService;
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
 * REST endpoints for restorations.
 */
@RestController
@RequestMapping("/api/restorations")
@RequiredArgsConstructor
public class RestorationController {

    private final RestorationService service;

    @GetMapping
    public ApiResponse<PageResponse<RestorationResponse>> list(
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/by-artwork/{artworkId}")
    public ApiResponse<PageResponse<RestorationResponse>> byArtwork(
            @PathVariable Long artworkId,
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        return ApiResponse.ok(service.listByArtwork(artworkId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<RestorationResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RestorationResponse>> create(@Valid @RequestBody RestorationRequest request) {
        RestorationResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Restoration created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RestorationResponse> update(@PathVariable Long id, @Valid @RequestBody RestorationRequest request) {
        return ApiResponse.ok(service.update(id, request), "Restoration updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Restoration deleted successfully");
    }
}
