package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.LoanRequest;
import com.artgallery.dto.LoanResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Loan;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.ExhibitorRepository;
import com.artgallery.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Loan}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final ArtworkRepository artworkRepository;
    private final ExhibitorRepository exhibitorRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> list(Pageable pageable) {
        log.debug("Listing loans {}", pageable);
        return PageResponse.from(loanRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> listByArtwork(Long artworkId, Pageable pageable) {
        return PageResponse.from(loanRepository.findByArtworkId(artworkId, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LoanResponse get(Long id) {
        return mapper.toResponse(findLoan(id));
    }

    public LoanResponse create(LoanRequest request) {
        Loan loan = new Loan();
        apply(loan, request);
        Loan saved = loanRepository.save(loan);
        log.info("Created loan id={} artworkId={}", saved.getId(), request.artworkId());
        return mapper.toResponse(saved);
    }

    public LoanResponse update(Long id, LoanRequest request) {
        Loan loan = findLoan(id);
        apply(loan, request);
        Loan saved = loanRepository.save(loan);
        log.info("Updated loan id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Loan loan = findLoan(id);
        loanRepository.delete(loan);
        log.info("Deleted loan id={}", id);
    }

    private Loan findLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    private void apply(Loan loan, LoanRequest r) {
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw new BadRequestException("End date must be on or after the start date");
        }
        loan.setStartDate(r.startDate());
        loan.setEndDate(r.endDate());
        loan.setConditions(r.conditions());
        loan.setArtwork(artworkRepository.findById(r.artworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", r.artworkId())));
        loan.setExhibitor(exhibitorRepository.findById(r.exhibitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Exhibitor", r.exhibitorId())));
    }
}
