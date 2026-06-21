package com.artgallery.service;

import com.artgallery.dto.ArtworkRequest;
import com.artgallery.dto.ArtworkResponse;
import com.artgallery.common.PageResponse;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artwork;
import com.artgallery.repository.ArtistRepository;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.CollectionRepository;
import com.artgallery.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Artwork}: CRUD, paginated listing, search and
 * resolution of the artist/collection/location relations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtistRepository artistRepository;
    private final CollectionRepository collectionRepository;
    private final LocationRepository locationRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ArtworkResponse> list(Pageable pageable) {
        log.debug("Listing artworks {}", pageable);
        return PageResponse.from(artworkRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ArtworkResponse> search(String term, Pageable pageable) {
        log.debug("Searching artworks for '{}'", term);
        return PageResponse.from(artworkRepository.search(term, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ArtworkResponse get(Long id) {
        log.debug("Fetching artwork id={}", id);
        return mapper.toResponse(findArtwork(id));
    }

    public ArtworkResponse create(ArtworkRequest request) {
        Artwork artwork = new Artwork();
        apply(artwork, request);
        Artwork saved = artworkRepository.save(artwork);
        log.info("Created artwork id={} title='{}'", saved.getId(), saved.getTitle());
        return mapper.toResponse(saved);
    }

    public ArtworkResponse update(Long id, ArtworkRequest request) {
        Artwork artwork = findArtwork(id);
        apply(artwork, request);
        Artwork saved = artworkRepository.save(artwork);
        log.info("Updated artwork id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Artwork artwork = findArtwork(id);
        artworkRepository.delete(artwork);
        log.info("Deleted artwork id={}", id);
    }

    private Artwork findArtwork(Long id) {
        return artworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", id));
    }

    private void apply(Artwork artwork, ArtworkRequest r) {
        artwork.setTitle(r.title());
        artwork.setYearCreated(r.yearCreated());
        artwork.setMedium(r.medium());
        artwork.setEstimatedValue(r.estimatedValue());
        artwork.setArtist(artistRepository.findById(r.artistId())
                .orElseThrow(() -> new ResourceNotFoundException("Artist", r.artistId())));
        artwork.setCollection(r.collectionId() == null ? null
                : collectionRepository.findById(r.collectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Collection", r.collectionId())));
        artwork.setLocation(r.locationId() == null ? null
                : locationRepository.findById(r.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", r.locationId())));
    }
}
