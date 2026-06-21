package com.artgallery.repository;

import com.artgallery.model.Insurance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceRepository extends JpaRepository<Insurance, Long> {

    @Override
    @EntityGraph(attributePaths = {"artwork", "policy"})
    Page<Insurance> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"artwork", "policy"})
    Page<Insurance> findByArtworkId(Long artworkId, Pageable pageable);

    @EntityGraph(attributePaths = {"artwork", "policy"})
    Page<Insurance> findByPolicyId(Long policyId, Pageable pageable);

    long countByPolicyId(Long policyId);
}
