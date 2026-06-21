package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.RestorationRequest;
import com.artgallery.dto.RestorationResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Restoration;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.RestorationRepository;
import com.artgallery.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Restoration}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RestorationService {

    private final RestorationRepository restorationRepository;
    private final ArtworkRepository artworkRepository;
    private final StaffRepository staffRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<RestorationResponse> list(Pageable pageable) {
        log.debug("Listing restorations {}", pageable);
        return PageResponse.from(restorationRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<RestorationResponse> listByArtwork(Long artworkId, Pageable pageable) {
        return PageResponse.from(restorationRepository.findByArtworkId(artworkId, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public RestorationResponse get(Long id) {
        return mapper.toResponse(findRestoration(id));
    }

    public RestorationResponse create(RestorationRequest request) {
        Restoration restoration = new Restoration();
        apply(restoration, request);
        Restoration saved = restorationRepository.save(restoration);
        log.info("Created restoration id={} artworkId={}", saved.getId(), request.artworkId());
        return mapper.toResponse(saved);
    }

    public RestorationResponse update(Long id, RestorationRequest request) {
        Restoration restoration = findRestoration(id);
        apply(restoration, request);
        Restoration saved = restorationRepository.save(restoration);
        log.info("Updated restoration id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Restoration restoration = findRestoration(id);
        restorationRepository.delete(restoration);
        log.info("Deleted restoration id={}", id);
    }

    private Restoration findRestoration(Long id) {
        return restorationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restoration", id));
    }

    private void apply(Restoration restoration, RestorationRequest r) {
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw new BadRequestException("End date must be on or after the start date");
        }
        restoration.setStartDate(r.startDate());
        restoration.setEndDate(r.endDate());
        restoration.setDescription(r.description());
        restoration.setArtwork(artworkRepository.findById(r.artworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", r.artworkId())));
        restoration.setStaff(staffRepository.findById(r.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", r.staffId())));
    }
}
