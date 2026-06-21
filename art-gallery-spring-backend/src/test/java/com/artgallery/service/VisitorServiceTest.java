package com.artgallery.service;

import com.artgallery.dto.VisitorRequest;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Visitor;
import com.artgallery.repository.VisitorRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisitorService")
class VisitorServiceTest {

    @Mock
    private VisitorRepository visitorRepository;

    private VisitorService service;

    @BeforeEach
    void setUp() {
        service = new VisitorService(visitorRepository, new DtoMapper());
    }

    private Visitor visitor(long id, String name, String email) {
        return Visitor.builder().id(id).name(name).email(email).phone("123")
                .membershipType("Gold").joinDate(LocalDate.of(2022, 1, 1)).build();
    }

    private VisitorRequest request(String name, String email) {
        return new VisitorRequest(name, email, "555", "Silver", LocalDate.of(2023, 1, 1));
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(visitorRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(visitor(1, "Ana", "a@x.com")), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search() delegates to the repository")
    void search() {
        Pageable pageable = PageRequest.of(0, 10);
        when(visitorRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                eq("An"), eq("An"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(visitor(1, "Ana", "a@x.com")), pageable, 1));
        assertThat(service.search("An", pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(visitorRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() persists a visitor with a unique email")
    void create() {
        when(visitorRepository.existsByEmailIgnoreCase("new@x.com")).thenReturn(false);
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(inv -> {
            Visitor v = inv.getArgument(0);
            v.setId(5L);
            return v;
        });
        assertThat(service.create(request("Bob", "new@x.com")).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create() skips the duplicate check when email is blank")
    void createNoEmail() {
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(request("Bob", null));
        verify(visitorRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("create() rejects a duplicate email")
    void createDuplicateEmail() {
        when(visitorRepository.existsByEmailIgnoreCase("dup@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("Bob", "dup@x.com")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(visitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        Visitor existing = visitor(3, "Old", "old@x.com");
        when(visitorRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(3L, request("New", "new@x.com")).name()).isEqualTo("New");
    }

    @Test
    @DisplayName("delete() removes the visitor")
    void delete() {
        Visitor existing = visitor(3, "Ana", "a@x.com");
        when(visitorRepository.findById(3L)).thenReturn(Optional.of(existing));
        service.delete(3L);
        verify(visitorRepository).delete(existing);
    }
}
