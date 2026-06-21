package com.artgallery.repository;

import com.artgallery.model.InsurancePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {

    Page<InsurancePolicy> findByProviderContainingIgnoreCase(String provider, Pageable pageable);
}
