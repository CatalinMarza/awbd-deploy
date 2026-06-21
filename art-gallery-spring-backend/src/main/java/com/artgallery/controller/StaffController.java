package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.StaffRequest;
import com.artgallery.dto.StaffResponse;
import com.artgallery.service.StaffService;
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
 * REST endpoints for staff members. Supports pagination &amp; sorting
 * (e.g. {@code ?sort=name,asc&sort=hireDate,desc}).
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService service;

    @GetMapping
    public ApiResponse<PageResponse<StaffResponse>> list(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<StaffResponse>> search(
            @RequestParam("q") String q,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ApiResponse.ok(service.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<StaffResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StaffResponse>> create(@Valid @RequestBody StaffRequest request) {
        StaffResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Staff member created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StaffResponse> update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return ApiResponse.ok(service.update(id, request), "Staff member updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Staff member deleted successfully");
    }
}
