package com.artgallery.service;

import com.artgallery.dto.CollectionRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Collection;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionService")
class CollectionServiceTest {

    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private ArtworkRepository artworkRepository;

    private CollectionService service;

    @BeforeEach
    void setUp() {
        service = new CollectionService(collectionRepository, artworkRepository, new DtoMapper());
    }

    private Collection collection(long id, String name) {
        return Collection.builder().id(id).name(name).description("d").createdDate(LocalDate.now()).build();
    }

    private CollectionRequest request(String name) {
        return new CollectionRequest(name, "Modern works", LocalDate.of(2020, 1, 1));
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(collectionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(collection(1, "A"), collection(2, "B")), pageable, 2));

        assertThat(service.list(pageable).totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("get() returns the mapped collection")
    void get() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection(1, "A")));
        assertThat(service.get(1L).name()).isEqualTo("A");
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(collectionRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() persists a unique collection")
    void create() {
        when(collectionRepository.existsByNameIgnoreCase("Modern")).thenReturn(false);
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> {
            Collection c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        assertThat(service.create(request("Modern")).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create() rejects duplicate name")
    void createDuplicate() {
        when(collectionRepository.existsByNameIgnoreCase("Modern")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("Modern")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(collectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        Collection existing = collection(3, "Old");
        when(collectionRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(3L, request("New")).name()).isEqualTo("New");
    }

    @Test
    @DisplayName("delete() succeeds with no artworks")
    void delete() {
        Collection existing = collection(3, "A");
        when(collectionRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(artworkRepository.countByCollectionId(3L)).thenReturn(0L);
        service.delete(3L);
        verify(collectionRepository).delete(existing);
    }

    @Test
    @DisplayName("delete() blocked by referencing artworks")
    void deleteBlocked() {
        when(collectionRepository.findById(3L)).thenReturn(Optional.of(collection(3, "A")));
        when(artworkRepository.countByCollectionId(3L)).thenReturn(2L);
        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(BadRequestException.class);
        verify(collectionRepository, never()).delete(any());
    }
}
