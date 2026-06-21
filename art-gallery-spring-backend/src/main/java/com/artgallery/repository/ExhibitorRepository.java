package com.artgallery.repository;

import com.artgallery.model.Exhibitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExhibitorRepository extends JpaRepository<Exhibitor, Long> {

    Page<Exhibitor> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
