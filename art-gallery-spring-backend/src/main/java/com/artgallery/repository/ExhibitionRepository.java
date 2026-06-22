package com.artgallery.repository;

import com.artgallery.model.Exhibition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {

    @Override
    @EntityGraph(attributePaths = {"exhibitor"})
    Page<Exhibition> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"exhibitor"})
    Page<Exhibition> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @EntityGraph(attributePaths = {"exhibitor"})
    Page<Exhibition> findByExhibitorId(Long exhibitorId, Pageable pageable);

    long countByExhibitorId(Long exhibitorId);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
            FROM artwork_exhibition
            WHERE exhibition_id = :exhibitionId
              AND artwork_id = :artworkId
            """, nativeQuery = true)
    boolean existsArtworkInExhibition(
            @Param("exhibitionId") Long exhibitionId,
            @Param("artworkId") Long artworkId
    );
}
