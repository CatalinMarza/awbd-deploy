package com.artgallery.service;

import com.artgallery.dto.LoanRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artwork;
import com.artgallery.model.Exhibitor;
import com.artgallery.model.Loan;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.ExhibitorRepository;
import com.artgallery.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.artgallery.model.Loan;
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
@DisplayName("LoanService")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private ExhibitorRepository exhibitorRepository;

    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(loanRepository, artworkRepository, exhibitorRepository, new DtoMapper());
    }

    private void stubRelations() {
        lenient().when(artworkRepository.findById(1L))
                .thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        lenient().when(exhibitorRepository.findById(2L))
                .thenReturn(Optional.of(Exhibitor.builder().id(2L).name("Louvre").build()));
    }

    @Test
    @DisplayName("create() succeeds when endDate is after startDate")
    void createValidDates() {
        stubRelations();
        LoanRequest request = new LoanRequest(1L, 2L,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1), "Climate controlled");
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(9L);
            return l;
        });

        var response = service.create(request);

        assertThat(response.id()).isEqualTo(9L);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("create() allows a null endDate (open-ended loan)")
    void createNullEndDate() {
        stubRelations();
        LoanRequest request = new LoanRequest(1L, 2L, LocalDate.of(2024, 1, 1), null, null);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(request);

        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("create() rejects an endDate before the startDate")
    void createInvalidDateRange() {
        LoanRequest request = new LoanRequest(1L, 2L,
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1), null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("on or after");
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() fails when the artwork is missing")
    void createMissingArtwork() {
        when(artworkRepository.findById(1L)).thenReturn(Optional.empty());
        LoanRequest request = new LoanRequest(1L, 2L, LocalDate.of(2024, 1, 1), null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Artwork");
    }

    @Test
    @DisplayName("get() throws for a missing loan")
    void getMissing() {
        when(loanRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan");
    }

    private Loan sampleLoan(long id) {
        return Loan.builder().id(id)
                .artwork(Artwork.builder().id(1L).title("X").build())
                .exhibitor(Exhibitor.builder().id(2L).name("Louvre").build())
                .startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2024, 6, 1)).build();
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(loanRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleLoan(1)), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByArtwork() delegates to the repository")
    void listByArtwork() {
        Pageable pageable = PageRequest.of(0, 10);
        when(loanRepository.findByArtworkId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleLoan(1)), pageable, 1));
        assertThat(service.listByArtwork(1L, pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        stubRelations();
        when(loanRepository.findById(3L)).thenReturn(Optional.of(sampleLoan(3)));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        service.update(3L, new LoanRequest(1L, 2L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 1), "ok"));
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("delete() removes the loan")
    void delete() {
        Loan existing = sampleLoan(3);
        when(loanRepository.findById(3L)).thenReturn(Optional.of(existing));
        service.delete(3L);
        verify(loanRepository).delete(existing);
    }
}
