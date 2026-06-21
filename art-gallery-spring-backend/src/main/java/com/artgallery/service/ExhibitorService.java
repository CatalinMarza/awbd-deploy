package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.ExhibitorRequest;
import com.artgallery.dto.ExhibitorResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Exhibitor;
import com.artgallery.repository.ExhibitionRepository;
import com.artgallery.repository.ExhibitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the {@link Exhibitor} lookup entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExhibitorService {

    private final ExhibitorRepository exhibitorRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ExhibitorResponse> list(Pageable pageable) {
        log.debug("Listing exhibitors {}", pageable);
        return PageResponse.from(exhibitorRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExhibitorResponse> search(String term, Pageable pageable) {
        return PageResponse.from(
                exhibitorRepository.findByNameContainingIgnoreCase(term, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ExhibitorResponse get(Long id) {
        return mapper.toResponse(findExhibitor(id));
    }

    public ExhibitorResponse create(ExhibitorRequest request) {
        if (exhibitorRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("An exhibitor named '%s' already exists".formatted(request.name()));
        }
        Exhibitor exhibitor = new Exhibitor();
        apply(exhibitor, request);
        Exhibitor saved = exhibitorRepository.save(exhibitor);
        log.info("Created exhibitor id={} name='{}'", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    public ExhibitorResponse update(Long id, ExhibitorRequest request) {
        Exhibitor exhibitor = findExhibitor(id);
        apply(exhibitor, request);
        Exhibitor saved = exhibitorRepository.save(exhibitor);
        log.info("Updated exhibitor id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Exhibitor exhibitor = findExhibitor(id);
        long exhibitions = exhibitionRepository.countByExhibitorId(id);
        if (exhibitions > 0) {
            throw new BadRequestException(
                    "Cannot delete exhibitor: %d exhibition(s) still reference it".formatted(exhibitions));
        }
        exhibitorRepository.delete(exhibitor);
        log.info("Deleted exhibitor id={}", id);
    }

    private Exhibitor findExhibitor(Long id) {
        return exhibitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exhibitor", id));
    }

    private void apply(Exhibitor exhibitor, ExhibitorRequest r) {
        exhibitor.setName(r.name());
        exhibitor.setAddress(r.address());
        exhibitor.setCity(r.city());
        exhibitor.setContactInfo(r.contactInfo());
    }
}
