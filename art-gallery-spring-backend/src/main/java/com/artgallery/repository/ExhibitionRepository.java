package com.artgallery.repository;

import com.artgallery.model.Exhibition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {

    @Override
    @EntityGraph(attributePaths = {"exhibitor"})
    Page<Exhibition> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"exhibitor"})
    Page<Exhibition> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @EntityGraph(attributePaths = {"exhibitor"})
    Page<Exhibition> findByExhibitorId(Long exhibitorId, Pageable pageable);

    long countByExhibitorId(Long exhibitorId);
}
