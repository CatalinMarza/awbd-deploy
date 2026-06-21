package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.CollectionRequest;
import com.artgallery.dto.CollectionResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Collection;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the {@link Collection} lookup entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ArtworkRepository artworkRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<CollectionResponse> list(Pageable pageable) {
        log.debug("Listing collections {}", pageable);
        return PageResponse.from(collectionRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CollectionResponse get(Long id) {
        return mapper.toResponse(findCollection(id));
    }

    public CollectionResponse create(CollectionRequest request) {
        if (collectionRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A collection named '%s' already exists".formatted(request.name()));
        }
        Collection collection = new Collection();
        apply(collection, request);
        Collection saved = collectionRepository.save(collection);
        log.info("Created collection id={} name='{}'", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    public CollectionResponse update(Long id, CollectionRequest request) {
        Collection collection = findCollection(id);
        apply(collection, request);
        Collection saved = collectionRepository.save(collection);
        log.info("Updated collection id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Collection collection = findCollection(id);
        long artworks = artworkRepository.countByCollectionId(id);
        if (artworks > 0) {
            throw new BadRequestException("Cannot delete collection: %d artwork(s) still reference it".formatted(artworks));
        }
        collectionRepository.delete(collection);
        log.info("Deleted collection id={}", id);
    }

    private Collection findCollection(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
    }

    private void apply(Collection collection, CollectionRequest r) {
        collection.setName(r.name());
        collection.setDescription(r.description());
        collection.setCreatedDate(r.createdDate());
    }
}
