package com.artgallery.repository;

import com.artgallery.model.Restoration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestorationRepository extends JpaRepository<Restoration, Long> {

    @Override
    @EntityGraph(attributePaths = {"artwork", "staff"})
    Page<Restoration> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"artwork", "staff"})
    Page<Restoration> findByArtworkId(Long artworkId, Pageable pageable);

    @EntityGraph(attributePaths = {"artwork", "staff"})
    Page<Restoration> findByStaffId(Long staffId, Pageable pageable);
}
