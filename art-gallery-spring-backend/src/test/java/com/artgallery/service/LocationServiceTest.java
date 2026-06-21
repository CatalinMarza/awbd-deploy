package com.artgallery.service;

import com.artgallery.dto.LocationRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Location;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService")
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ArtworkRepository artworkRepository;

    private LocationService service;

    @BeforeEach
    void setUp() {
        service = new LocationService(locationRepository, artworkRepository, new DtoMapper());
    }

    private Location location(long id, String name) {
        return Location.builder().id(id).name(name).galleryRoom("R1").type("Gallery").capacity(50).build();
    }

    private LocationRequest request(String name) {
        return new LocationRequest(name, "R2", "Vault", 30);
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(locationRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(location(1, "Hall")), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("get() returns the mapped location")
    void get() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location(1, "Hall")));
        assertThat(service.get(1L).name()).isEqualTo("Hall");
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(locationRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() persists a unique location")
    void create() {
        when(locationRepository.existsByNameIgnoreCase("Vault")).thenReturn(false);
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            l.setId(5L);
            return l;
        });
        assertThat(service.create(request("Vault")).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create() rejects duplicate name")
    void createDuplicate() {
        when(locationRepository.existsByNameIgnoreCase("Vault")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("Vault")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(locationRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        Location existing = location(3, "Old");
        when(locationRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(3L, request("New")).name()).isEqualTo("New");
    }

    @Test
    @DisplayName("delete() succeeds with no artworks")
    void delete() {
        Location existing = location(3, "Hall");
        when(locationRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(artworkRepository.countByLocationId(3L)).thenReturn(0L);
        service.delete(3L);
        verify(locationRepository).delete(existing);
    }

    @Test
    @DisplayName("delete() blocked by referencing artworks")
    void deleteBlocked() {
        when(locationRepository.findById(3L)).thenReturn(Optional.of(location(3, "Hall")));
        when(artworkRepository.countByLocationId(3L)).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(BadRequestException.class);
        verify(locationRepository, never()).delete(any());
    }
}
