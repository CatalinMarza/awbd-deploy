package com.artgallery.repository;

import com.artgallery.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Override
    @EntityGraph(attributePaths = {"artwork", "exhibitor"})
    Page<Loan> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"artwork", "exhibitor"})
    Page<Loan> findByArtworkId(Long artworkId, Pageable pageable);

    @EntityGraph(attributePaths = {"artwork", "exhibitor"})
    Page<Loan> findByExhibitorId(Long exhibitorId, Pageable pageable);
}
