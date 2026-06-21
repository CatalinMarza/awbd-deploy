package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.LoanRequest;
import com.artgallery.dto.LoanResponse;
import com.artgallery.service.LoanService;
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
 * REST endpoints for loans. Supports pagination &amp; sorting
 * (e.g. {@code ?sort=startDate,desc&sort=id,asc}).
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService service;

    @GetMapping
    public ApiResponse<PageResponse<LoanResponse>> list(
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/by-artwork/{artworkId}")
    public ApiResponse<PageResponse<LoanResponse>> byArtwork(
            @PathVariable Long artworkId,
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        return ApiResponse.ok(service.listByArtwork(artworkId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<LoanResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> create(@Valid @RequestBody LoanRequest request) {
        LoanResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Loan created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LoanResponse> update(@PathVariable Long id, @Valid @RequestBody LoanRequest request) {
        return ApiResponse.ok(service.update(id, request), "Loan updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Loan deleted successfully");
    }
}
