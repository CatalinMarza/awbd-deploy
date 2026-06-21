package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.AcquisitionRequest;
import com.artgallery.dto.AcquisitionResponse;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Acquisition;
import com.artgallery.repository.AcquisitionRepository;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Acquisition}. Enforces the {@code @OneToOne} rule:
 * an artwork can have at most one acquisition record.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AcquisitionService {

    private final AcquisitionRepository acquisitionRepository;
    private final ArtworkRepository artworkRepository;
    private final StaffRepository staffRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<AcquisitionResponse> list(Pageable pageable) {
        log.debug("Listing acquisitions {}", pageable);
        return PageResponse.from(acquisitionRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AcquisitionResponse get(Long id) {
        return mapper.toResponse(findAcquisition(id));
    }

    public AcquisitionResponse create(AcquisitionRequest request) {
        if (acquisitionRepository.existsByArtworkId(request.artworkId())) {
            throw new DuplicateResourceException(
                    "Artwork id %s already has an acquisition record".formatted(request.artworkId()));
        }
        Acquisition acquisition = new Acquisition();
        apply(acquisition, request, null);
        Acquisition saved = acquisitionRepository.save(acquisition);
        log.info("Created acquisition id={} artworkId={}", saved.getId(), request.artworkId());
        return mapper.toResponse(saved);
    }

    public AcquisitionResponse update(Long id, AcquisitionRequest request) {
        Acquisition acquisition = findAcquisition(id);
        apply(acquisition, request, id);
        Acquisition saved = acquisitionRepository.save(acquisition);
        log.info("Updated acquisition id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Acquisition acquisition = findAcquisition(id);
        acquisitionRepository.delete(acquisition);
        log.info("Deleted acquisition id={}", id);
    }

    private Acquisition findAcquisition(Long id) {
        return acquisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acquisition", id));
    }

    private void apply(Acquisition acquisition, AcquisitionRequest r, Long currentId) {
        acquisitionRepository.findByArtworkId(r.artworkId()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new DuplicateResourceException(
                        "Artwork id %s already has an acquisition record".formatted(r.artworkId()));
            }
        });
        acquisition.setAcquisitionDate(r.acquisitionDate());
        acquisition.setAcquisitionType(r.acquisitionType());
        acquisition.setPrice(r.price());
        acquisition.setArtwork(artworkRepository.findById(r.artworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", r.artworkId())));
        acquisition.setStaff(staffRepository.findById(r.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", r.staffId())));
    }
}
