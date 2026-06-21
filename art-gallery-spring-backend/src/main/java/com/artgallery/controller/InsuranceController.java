package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.InsuranceRequest;
import com.artgallery.dto.InsuranceResponse;
import com.artgallery.service.InsuranceService;
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
 * REST endpoints for insurance records.
 */
@RestController
@RequestMapping("/api/insurances")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService service;

    @GetMapping
    public ApiResponse<PageResponse<InsuranceResponse>> list(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/by-artwork/{artworkId}")
    public ApiResponse<PageResponse<InsuranceResponse>> byArtwork(
            @PathVariable Long artworkId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.ok(service.listByArtwork(artworkId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<InsuranceResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceResponse>> create(@Valid @RequestBody InsuranceRequest request) {
        InsuranceResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Insurance created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InsuranceResponse> update(@PathVariable Long id, @Valid @RequestBody InsuranceRequest request) {
        return ApiResponse.ok(service.update(id, request), "Insurance updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Insurance deleted successfully");
    }
}
