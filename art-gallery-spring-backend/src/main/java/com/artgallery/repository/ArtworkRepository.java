package com.artgallery.repository;

import com.artgallery.model.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    @Override
    @EntityGraph(attributePaths = {"artist", "collection", "location"})
    Page<Artwork> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"artist", "collection", "location"})
    @Query("""
            select a from Artwork a
            where lower(a.title) like lower(concat('%', :term, '%'))
               or lower(a.artist.name) like lower(concat('%', :term, '%'))
            """)
    Page<Artwork> search(@Param("term") String term, Pageable pageable);

    @EntityGraph(attributePaths = {"artist", "collection", "location"})
    Page<Artwork> findByArtistId(Long artistId, Pageable pageable);

    @EntityGraph(attributePaths = {"artist", "collection", "location"})
    Page<Artwork> findByCollectionId(Long collectionId, Pageable pageable);

    long countByArtistId(Long artistId);

    long countByCollectionId(Long collectionId);

    long countByLocationId(Long locationId);
}
