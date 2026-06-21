package com.artgallery.service;

import com.artgallery.dto.ExhibitorRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Exhibitor;
import com.artgallery.repository.ExhibitionRepository;
import com.artgallery.repository.ExhibitorRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExhibitorService")
class ExhibitorServiceTest {

    @Mock
    private ExhibitorRepository exhibitorRepository;
    @Mock
    private ExhibitionRepository exhibitionRepository;

    private ExhibitorService service;

    @BeforeEach
    void setUp() {
        service = new ExhibitorService(exhibitorRepository, exhibitionRepository, new DtoMapper());
    }

    private Exhibitor exhibitor(long id, String name) {
        return Exhibitor.builder().id(id).name(name).address("a").city("Paris").contactInfo("c").build();
    }

    private ExhibitorRequest request(String name) {
        return new ExhibitorRequest(name, "1 Rue", "Paris", "info@x.com");
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(exhibitorRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(exhibitor(1, "Louvre")), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search() delegates to the repository")
    void search() {
        Pageable pageable = PageRequest.of(0, 10);
        when(exhibitorRepository.findByNameContainingIgnoreCase(eq("Lou"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exhibitor(1, "Louvre")), pageable, 1));
        assertThat(service.search("Lou", pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(exhibitorRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() persists a unique exhibitor")
    void create() {
        when(exhibitorRepository.existsByNameIgnoreCase("Tate")).thenReturn(false);
        when(exhibitorRepository.save(any(Exhibitor.class))).thenAnswer(inv -> {
            Exhibitor e = inv.getArgument(0);
            e.setId(5L);
            return e;
        });
        assertThat(service.create(request("Tate")).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create() rejects duplicate name")
    void createDuplicate() {
        when(exhibitorRepository.existsByNameIgnoreCase("Tate")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("Tate")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(exhibitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        Exhibitor existing = exhibitor(3, "Old");
        when(exhibitorRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(exhibitorRepository.save(any(Exhibitor.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(3L, request("New")).name()).isEqualTo("New");
    }

    @Test
    @DisplayName("delete() blocked by referencing exhibitions")
    void deleteBlocked() {
        when(exhibitorRepository.findById(3L)).thenReturn(Optional.of(exhibitor(3, "Louvre")));
        when(exhibitionRepository.countByExhibitorId(3L)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(BadRequestException.class);
        verify(exhibitorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete() succeeds with no exhibitions")
    void delete() {
        Exhibitor existing = exhibitor(3, "Louvre");
        when(exhibitorRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(exhibitionRepository.countByExhibitorId(3L)).thenReturn(0L);
        service.delete(3L);
        verify(exhibitorRepository).delete(existing);
    }
}
