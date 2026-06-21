package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.ArtworkResponse;
import com.artgallery.dto.ExhibitionRequest;
import com.artgallery.dto.ExhibitionResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artwork;
import com.artgallery.model.Exhibition;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.ExhibitionRepository;
import com.artgallery.repository.ExhibitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for {@link Exhibition}, including management of the
 * {@code @ManyToMany} association with {@link Artwork}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitorRepository exhibitorRepository;
    private final ArtworkRepository artworkRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ExhibitionResponse> list(Pageable pageable) {
        log.debug("Listing exhibitions {}", pageable);
        return PageResponse.from(exhibitionRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExhibitionResponse> search(String term, Pageable pageable) {
        log.debug("Searching exhibitions for '{}'", term);
        return PageResponse.from(
                exhibitionRepository.findByTitleContainingIgnoreCase(term, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ExhibitionResponse get(Long id) {
        return mapper.toResponse(findExhibition(id));
    }

    public ExhibitionResponse create(ExhibitionRequest request) {
        Exhibition exhibition = new Exhibition();
        apply(exhibition, request);
        Exhibition saved = exhibitionRepository.save(exhibition);
        log.info("Created exhibition id={} title='{}'", saved.getId(), saved.getTitle());
        return mapper.toResponse(saved);
    }

    public ExhibitionResponse update(Long id, ExhibitionRequest request) {
        Exhibition exhibition = findExhibition(id);
        apply(exhibition, request);
        Exhibition saved = exhibitionRepository.save(exhibition);
        log.info("Updated exhibition id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Exhibition exhibition = findExhibition(id);
        exhibitionRepository.delete(exhibition);
        log.info("Deleted exhibition id={}", id);
    }

    /** Lists the artworks that belong to an exhibition ({@code @ManyToMany} read). */
    @Transactional(readOnly = true)
    public List<ArtworkResponse> listArtworks(Long exhibitionId) {
        Exhibition exhibition = findExhibition(exhibitionId);
        return exhibition.getArtworks().stream().map(mapper::toResponse).toList();
    }

    /** Adds an artwork to an exhibition (owning side is {@link Artwork}). */
    public void addArtwork(Long exhibitionId, Long artworkId) {
        Exhibition exhibition = findExhibition(exhibitionId);
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", artworkId));
        if (!artwork.getExhibitions().contains(exhibition)) {
            artwork.getExhibitions().add(exhibition);
            artworkRepository.save(artwork);
            log.info("Linked artwork id={} to exhibition id={}", artworkId, exhibitionId);
        }
    }

    /** Removes an artwork from an exhibition. */
    public void removeArtwork(Long exhibitionId, Long artworkId) {
        Exhibition exhibition = findExhibition(exhibitionId);
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", artworkId));
        if (artwork.getExhibitions().remove(exhibition)) {
            artworkRepository.save(artwork);
            log.info("Unlinked artwork id={} from exhibition id={}", artworkId, exhibitionId);
        }
    }

    private Exhibition findExhibition(Long id) {
        return exhibitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exhibition", id));
    }

    private void apply(Exhibition exhibition, ExhibitionRequest r) {
        if (r.endDate().isBefore(r.startDate())) {
            throw new BadRequestException("End date must be on or after the start date");
        }
        exhibition.setTitle(r.title());
        exhibition.setStartDate(r.startDate());
        exhibition.setEndDate(r.endDate());
        exhibition.setDescription(r.description());
        exhibition.setExhibitor(exhibitorRepository.findById(r.exhibitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Exhibitor", r.exhibitorId())));
    }
}
