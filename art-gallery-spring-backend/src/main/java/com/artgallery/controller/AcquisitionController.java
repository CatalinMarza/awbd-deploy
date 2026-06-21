package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.AcquisitionRequest;
import com.artgallery.dto.AcquisitionResponse;
import com.artgallery.service.AcquisitionService;
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
 * REST endpoints for acquisitions (the {@code @OneToOne} record of how an
 * artwork was acquired).
 */
@RestController
@RequestMapping("/api/acquisitions")
@RequiredArgsConstructor
public class AcquisitionController {

    private final AcquisitionService service;

    @GetMapping
    public ApiResponse<PageResponse<AcquisitionResponse>> list(
            @PageableDefault(size = 10, sort = "acquisitionDate") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AcquisitionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AcquisitionResponse>> create(@Valid @RequestBody AcquisitionRequest request) {
        AcquisitionResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Acquisition created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AcquisitionResponse> update(@PathVariable Long id, @Valid @RequestBody AcquisitionRequest request) {
        return ApiResponse.ok(service.update(id, request), "Acquisition updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Acquisition deleted successfully");
    }
}
