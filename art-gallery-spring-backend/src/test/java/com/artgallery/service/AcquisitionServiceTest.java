package com.artgallery.service;

import com.artgallery.dto.AcquisitionRequest;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Acquisition;
import com.artgallery.model.Artwork;
import com.artgallery.model.Staff;
import com.artgallery.repository.AcquisitionRepository;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcquisitionService (@OneToOne uniqueness)")
class AcquisitionServiceTest {

    @Mock
    private AcquisitionRepository acquisitionRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private StaffRepository staffRepository;

    private AcquisitionService service;

    @BeforeEach
    void setUp() {
        service = new AcquisitionService(acquisitionRepository, artworkRepository, staffRepository, new DtoMapper());
    }

    private AcquisitionRequest request() {
        return new AcquisitionRequest(1L, LocalDate.of(2024, 1, 1), "Purchase", new BigDecimal("5000.00"), 3L);
    }

    private void stubRelations() {
        lenient().when(artworkRepository.findById(1L))
                .thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        lenient().when(staffRepository.findById(3L))
                .thenReturn(Optional.of(Staff.builder().id(3L).name("Curator").build()));
    }

    @Test
    @DisplayName("create() persists when the artwork has no acquisition yet")
    void createSucceeds() {
        stubRelations();
        when(acquisitionRepository.existsByArtworkId(1L)).thenReturn(false);
        when(acquisitionRepository.findByArtworkId(1L)).thenReturn(Optional.empty());
        when(acquisitionRepository.save(any(Acquisition.class))).thenAnswer(inv -> {
            Acquisition a = inv.getArgument(0);
            a.setId(11L);
            return a;
        });

        var response = service.create(request());

        assertThat(response.id()).isEqualTo(11L);
        verify(acquisitionRepository).save(any(Acquisition.class));
    }

    @Test
    @DisplayName("create() rejects a second acquisition for the same artwork")
    void createDuplicateThrows() {
        when(acquisitionRepository.existsByArtworkId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already has an acquisition");
        verify(acquisitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() fails when the staff member is missing")
    void createMissingStaff() {
        when(acquisitionRepository.existsByArtworkId(1L)).thenReturn(false);
        when(acquisitionRepository.findByArtworkId(1L)).thenReturn(Optional.empty());
        when(artworkRepository.findById(1L)).thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        when(staffRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Staff");
    }

    @Test
    @DisplayName("update() allows keeping the same artwork on the current record")
    void updateSameArtwork() {
        Acquisition existing = Acquisition.builder().id(5L).build();
        when(acquisitionRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(acquisitionRepository.findByArtworkId(1L)).thenReturn(Optional.of(existing));
        when(artworkRepository.findById(1L)).thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        when(staffRepository.findById(3L)).thenReturn(Optional.of(Staff.builder().id(3L).name("Curator").build()));
        when(acquisitionRepository.save(any(Acquisition.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.update(5L, request());

        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("update() rejects moving the acquisition onto an artwork owned by another record")
    void updateConflictThrows() {
        Acquisition current = Acquisition.builder().id(5L).build();
        Acquisition other = Acquisition.builder().id(8L).build();
        when(acquisitionRepository.findById(5L)).thenReturn(Optional.of(current));
        when(acquisitionRepository.findByArtworkId(1L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(5L, request()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(acquisitionRepository, never()).save(any());
    }
}
