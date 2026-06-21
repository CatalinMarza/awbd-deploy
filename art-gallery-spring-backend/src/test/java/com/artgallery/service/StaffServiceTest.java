package com.artgallery.service;

import com.artgallery.dto.StaffRequest;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Staff;
import com.artgallery.repository.StaffRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaffService")
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    private StaffService service;

    @BeforeEach
    void setUp() {
        service = new StaffService(staffRepository, new DtoMapper());
    }

    private Staff staff(long id, String name) {
        return Staff.builder().id(id).name(name).role("Curator").hireDate(LocalDate.of(2020, 1, 1))
                .certificationLevel("Senior").build();
    }

    private StaffRequest request(String name) {
        return new StaffRequest(name, "Conservator", LocalDate.of(2021, 5, 1), "Junior");
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(staffRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(staff(1, "Ana"), staff(2, "Ben")), pageable, 2));
        assertThat(service.list(pageable).totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("search() delegates to the repository")
    void search() {
        Pageable pageable = PageRequest.of(0, 10);
        when(staffRepository.findByNameContainingIgnoreCaseOrRoleContainingIgnoreCase(
                eq("Cur"), eq("Cur"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(staff(1, "Ana")), pageable, 1));
        assertThat(service.search("Cur", pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("get() returns the mapped staff")
    void get() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff(1, "Ana")));
        assertThat(service.get(1L).name()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(staffRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() persists a new staff member")
    void create() {
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> {
            Staff s = inv.getArgument(0);
            s.setId(5L);
            return s;
        });
        assertThat(service.create(request("Cara")).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        Staff existing = staff(3, "Old");
        when(staffRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(3L, request("New")).name()).isEqualTo("New");
    }

    @Test
    @DisplayName("delete() removes the staff member")
    void delete() {
        Staff existing = staff(3, "Ana");
        when(staffRepository.findById(3L)).thenReturn(Optional.of(existing));
        service.delete(3L);
        verify(staffRepository).delete(existing);
    }
}
