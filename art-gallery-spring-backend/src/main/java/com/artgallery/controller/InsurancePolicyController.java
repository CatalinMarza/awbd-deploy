package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.dto.InsurancePolicyRequest;
import com.artgallery.dto.InsurancePolicyResponse;
import com.artgallery.service.InsurancePolicyService;
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
 * REST endpoints for insurance policies.
 */
@RestController
@RequestMapping("/api/insurance-policies")
@RequiredArgsConstructor
public class InsurancePolicyController {

    private final InsurancePolicyService service;

    @GetMapping
    public ApiResponse<PageResponse<InsurancePolicyResponse>> list(
            @PageableDefault(size = 10, sort = "provider") Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<InsurancePolicyResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InsurancePolicyResponse>> create(
            @Valid @RequestBody InsurancePolicyRequest request) {
        InsurancePolicyResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Insurance policy created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InsurancePolicyResponse> update(
            @PathVariable Long id, @Valid @RequestBody InsurancePolicyRequest request) {
        return ApiResponse.ok(service.update(id, request), "Insurance policy updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>ok(null, "Insurance policy deleted successfully");
    }
}
