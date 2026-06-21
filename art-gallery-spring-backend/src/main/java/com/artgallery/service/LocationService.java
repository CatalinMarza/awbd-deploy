package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.LocationRequest;
import com.artgallery.dto.LocationResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Location;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the {@link Location} lookup entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;
    private final ArtworkRepository artworkRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> list(Pageable pageable) {
        log.debug("Listing locations {}", pageable);
        return PageResponse.from(locationRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LocationResponse get(Long id) {
        return mapper.toResponse(findLocation(id));
    }

    public LocationResponse create(LocationRequest request) {
        if (locationRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A location named '%s' already exists".formatted(request.name()));
        }
        Location location = new Location();
        apply(location, request);
        Location saved = locationRepository.save(location);
        log.info("Created location id={} name='{}'", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    public LocationResponse update(Long id, LocationRequest request) {
        Location location = findLocation(id);
        apply(location, request);
        Location saved = locationRepository.save(location);
        log.info("Updated location id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Location location = findLocation(id);
        long artworks = artworkRepository.countByLocationId(id);
        if (artworks > 0) {
            throw new BadRequestException("Cannot delete location: %d artwork(s) still reference it".formatted(artworks));
        }
        locationRepository.delete(location);
        log.info("Deleted location id={}", id);
    }

    private Location findLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));
    }

    private void apply(Location location, LocationRequest r) {
        location.setName(r.name());
        location.setGalleryRoom(r.galleryRoom());
        location.setType(r.type());
        location.setCapacity(r.capacity());
    }
}
