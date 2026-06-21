package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.ArtistRequest;
import com.artgallery.dto.ArtistResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artist;
import com.artgallery.repository.ArtistRepository;
import com.artgallery.repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the {@link Artist} lookup entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ArtistResponse> list(Pageable pageable) {
        log.debug("Listing artists {}", pageable);
        return PageResponse.from(artistRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ArtistResponse> search(String term, Pageable pageable) {
        return PageResponse.from(artistRepository.findByNameContainingIgnoreCase(term, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ArtistResponse get(Long id) {
        return mapper.toResponse(findArtist(id));
    }

    public ArtistResponse create(ArtistRequest request) {
        if (artistRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("An artist named '%s' already exists".formatted(request.name()));
        }
        Artist artist = new Artist();
        apply(artist, request);
        Artist saved = artistRepository.save(artist);
        log.info("Created artist id={} name='{}'", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    public ArtistResponse update(Long id, ArtistRequest request) {
        Artist artist = findArtist(id);
        apply(artist, request);
        Artist saved = artistRepository.save(artist);
        log.info("Updated artist id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Artist artist = findArtist(id);
        long artworks = artworkRepository.countByArtistId(id);
        if (artworks > 0) {
            throw new BadRequestException("Cannot delete artist: %d artwork(s) still reference it".formatted(artworks));
        }
        artistRepository.delete(artist);
        log.info("Deleted artist id={}", id);
    }

    private Artist findArtist(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", id));
    }

    private void apply(Artist artist, ArtistRequest r) {
        artist.setName(r.name());
        artist.setNationality(r.nationality());
        artist.setBirthYear(r.birthYear());
        artist.setDeathYear(r.deathYear());
    }
}
