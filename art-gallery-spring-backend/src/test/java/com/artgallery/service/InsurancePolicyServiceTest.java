package com.artgallery.service;

import com.artgallery.dto.InsurancePolicyRequest;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.InsurancePolicy;
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
@DisplayName("InsurancePolicyService")
class InsurancePolicyServiceTest {

    @Mock
    private InsurancePolicyRepository policyRepository;
    @Mock
    private InsuranceRepository insuranceRepository;

    private InsurancePolicyService service;

    @BeforeEach
    void setUp() {
        service = new InsurancePolicyService(policyRepository, insuranceRepository, new DtoMapper());
    }

    private InsurancePolicy policy(long id) {
        return InsurancePolicy.builder().id(id).provider("AXA")
                .startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2025, 1, 1))
                .totalCoverageAmount(new BigDecimal("100000")).build();
    }

    private InsurancePolicyRequest request(LocalDate start, LocalDate end) {
        return new InsurancePolicyRequest("Allianz", start, end, new BigDecimal("50000"));
    }

    @Test
    @DisplayName("list() maps the page")
    void list() {
        Pageable pageable = PageRequest.of(0, 10);
        when(policyRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(policy(1)), pageable, 1));
        assertThat(service.list(pageable).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("get() throws when missing")
    void getMissing() {
        when(policyRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create() succeeds with a valid date range")
    void createValid() {
        when(policyRepository.save(any(InsurancePolicy.class))).thenAnswer(inv -> {
            InsurancePolicy p = inv.getArgument(0);
            p.setId(5L);
            return p;
        });
        assertThat(service.create(request(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1))).id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create() rejects an end date before the start date")
    void createInvalidDates() {
        assertThatThrownBy(() -> service.create(request(LocalDate.of(2025, 1, 1), LocalDate.of(2024, 1, 1))))
                .isInstanceOf(BadRequestException.class);
        verify(policyRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() applies the changes")
    void update() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy(1)));
        when(policyRepository.save(any(InsurancePolicy.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.update(1L, request(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1))).provider())
                .isEqualTo("Allianz");
    }

    @Test
    @DisplayName("delete() blocked by referencing insurances")
    void deleteBlocked() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy(1)));
        when(insuranceRepository.countByPolicyId(1L)).thenReturn(2L);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BadRequestException.class);
        verify(policyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete() succeeds with no insurances")
    void delete() {
        InsurancePolicy existing = policy(1);
        when(policyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(insuranceRepository.countByPolicyId(1L)).thenReturn(0L);
        service.delete(1L);
        verify(policyRepository).delete(existing);
    }
}
