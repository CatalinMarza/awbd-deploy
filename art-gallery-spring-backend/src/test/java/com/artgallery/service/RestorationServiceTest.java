package com.artgallery.service;

import com.artgallery.dto.RestorationRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artwork;
import com.artgallery.model.Restoration;
import com.artgallery.model.Staff;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.RestorationRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestorationService")
class RestorationServiceTest {

    @Mock
    private RestorationRepository restorationRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private StaffRepository staffRepository;

    private RestorationService service;

    @BeforeEach
    void setUp() {
        service = new RestorationService(restorationRepository, artworkRepository, staffRepository, new DtoMapper());
    }

    private Restoration restoration(long id) {
        return Restoration.builder().id(id)
                .artwork(Artwork.builder().id(1L).title("X").build())
                .staff(Staff.builder().id(3L).name("Curator").build())
                .startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2024, 2, 1))
                .description("clean").build();
    }

    private void stubRelations() {
        lenient().when(artworkRepository.findById(1L))
                .thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        lenient().when(staffRepository.findById(3L))
                .thenReturn(Optional.of(Staff.builder().id(3L).name("Curator").build()));
    }

    private RestorationRequest request(LocalDate start, LocalDate end) {
        return new RestorationRequest(1L, 3L, start, end, "Surface cleaning");
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(restorationRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(restoration(1)), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByArtwork() delegates to the repository")
    void listByArtwork() {
        Pageable pageable = PageRequest.of(0, 10);
        when(restorationRepository.findByArtworkId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(restoration(1)), pageable, 1));
        assertThat(service.listByArtwork(1L, pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(restorationRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() succeeds with a valid date range")
    void createValid() {
        stubRelations();
        when(restorationRepository.save(any(Restoration.class))).thenAnswer(inv -> {
            Restoration r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });
        assertThat(service.create(request(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1))).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create() allows a null end date")
    void createNullEnd() {
        stubRelations();
        when(restorationRepository.save(any(Restoration.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(request(LocalDate.of(2024, 1, 1), null));
        verify(restorationRepository).save(any(Restoration.class));
    }

    @Test
    @DisplayName("create() rejects an end date before the start date")
    void createInvalidDates() {
        assertThatThrownBy(() -> service.create(request(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 1, 1))))
                .isInstanceOf(BadRequestException.class);
        verify(restorationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() fails when the staff member is missing")
    void createMissingStaff() {
        when(artworkRepository.findById(1L)).thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        when(staffRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Staff");
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        stubRelations();
        when(restorationRepository.findById(3L)).thenReturn(Optional.of(restoration(3)));
        when(restorationRepository.save(any(Restoration.class))).thenAnswer(inv -> inv.getArgument(0));
        service.update(3L, request(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1)));
        verify(restorationRepository).save(any(Restoration.class));
    }

    @Test
    @DisplayName("delete() removes the restoration")
    void delete() {
        Restoration existing = restoration(3);
        when(restorationRepository.findById(3L)).thenReturn(Optional.of(existing));
        service.delete(3L);
        verify(restorationRepository).delete(existing);
    }
}
