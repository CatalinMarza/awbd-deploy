package com.artgallery.service;

import com.artgallery.dto.InsuranceRequest;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Artwork;
import com.artgallery.model.Insurance;
import com.artgallery.model.InsurancePolicy;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.InsurancePolicyRepository;
import com.artgallery.repository.InsuranceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceService")
class InsuranceServiceTest {

    @Mock
    private InsuranceRepository insuranceRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private InsurancePolicyRepository policyRepository;

    private InsuranceService service;

    @BeforeEach
    void setUp() {
        service = new InsuranceService(insuranceRepository, artworkRepository, policyRepository, new DtoMapper());
    }

    private Insurance insurance(long id) {
        return Insurance.builder().id(id)
                .artwork(Artwork.builder().id(1L).title("X").build())
                .policy(InsurancePolicy.builder().id(2L).provider("AXA").build())
                .insuredAmount(new BigDecimal("1000")).build();
    }

    private void stubRelations() {
        lenient().when(artworkRepository.findById(1L))
                .thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        lenient().when(policyRepository.findById(2L))
                .thenReturn(Optional.of(InsurancePolicy.builder().id(2L).provider("AXA").build()));
    }

    private InsuranceRequest request() {
        return new InsuranceRequest(1L, 2L, new BigDecimal("5000"));
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(insuranceRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(insurance(1)), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByArtwork() delegates to the repository")
    void listByArtwork() {
        Pageable pageable = PageRequest.of(0, 10);
        when(insuranceRepository.findByArtworkId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(insurance(1)), pageable, 1));
        assertThat(service.listByArtwork(1L, pageable).content()).hasSize(1);
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(insuranceRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() resolves the artwork and policy")
    void create() {
        stubRelations();
        when(insuranceRepository.save(any(Insurance.class))).thenAnswer(inv -> {
            Insurance i = inv.getArgument(0);
            i.setId(5L);
            return i;
        });
        assertThat(service.create(request()).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create() fails when the policy is missing")
    void createMissingPolicy() {
        when(artworkRepository.findById(1L)).thenReturn(Optional.of(Artwork.builder().id(1L).title("X").build()));
        when(policyRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("InsurancePolicy");
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        stubRelations();
        when(insuranceRepository.findById(3L)).thenReturn(Optional.of(insurance(3)));
        when(insuranceRepository.save(any(Insurance.class))).thenAnswer(inv -> inv.getArgument(0));
        service.update(3L, request());
        verify(insuranceRepository).save(any(Insurance.class));
    }

    @Test
    @DisplayName("delete() removes the insurance")
    void delete() {
        Insurance existing = insurance(3);
        when(insuranceRepository.findById(3L)).thenReturn(Optional.of(existing));
        service.delete(3L);
        verify(insuranceRepository).delete(existing);
    }
}
