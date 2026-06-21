package com.artgallery.repository;

import com.artgallery.model.Acquisition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcquisitionRepository extends JpaRepository<Acquisition, Long> {

    @Override
    @EntityGraph(attributePaths = {"artwork", "staff"})
    Page<Acquisition> findAll(Pageable pageable);

    boolean existsByArtworkId(Long artworkId);

    Optional<Acquisition> findByArtworkId(Long artworkId);
}
