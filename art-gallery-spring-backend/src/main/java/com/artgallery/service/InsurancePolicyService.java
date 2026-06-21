package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.InsurancePolicyRequest;
import com.artgallery.dto.InsurancePolicyResponse;
import com.artgallery.exception.BadRequestException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.InsurancePolicy;
import com.artgallery.repository.InsurancePolicyRepository;
import com.artgallery.repository.InsuranceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the {@link InsurancePolicy} lookup entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InsurancePolicyService {

    private final InsurancePolicyRepository policyRepository;
    private final InsuranceRepository insuranceRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<InsurancePolicyResponse> list(Pageable pageable) {
        log.debug("Listing insurance policies {}", pageable);
        return PageResponse.from(policyRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public InsurancePolicyResponse get(Long id) {
        return mapper.toResponse(findPolicy(id));
    }

    public InsurancePolicyResponse create(InsurancePolicyRequest request) {
        InsurancePolicy policy = new InsurancePolicy();
        apply(policy, request);
        InsurancePolicy saved = policyRepository.save(policy);
        log.info("Created insurance policy id={} provider='{}'", saved.getId(), saved.getProvider());
        return mapper.toResponse(saved);
    }

    public InsurancePolicyResponse update(Long id, InsurancePolicyRequest request) {
        InsurancePolicy policy = findPolicy(id);
        apply(policy, request);
        InsurancePolicy saved = policyRepository.save(policy);
        log.info("Updated insurance policy id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        InsurancePolicy policy = findPolicy(id);
        long insurances = insuranceRepository.countByPolicyId(id);
        if (insurances > 0) {
            throw new BadRequestException(
                    "Cannot delete policy: %d insurance record(s) still reference it".formatted(insurances));
        }
        policyRepository.delete(policy);
        log.info("Deleted insurance policy id={}", id);
    }

    private InsurancePolicy findPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsurancePolicy", id));
    }

    private void apply(InsurancePolicy policy, InsurancePolicyRequest r) {
        if (r.endDate().isBefore(r.startDate())) {
            throw new BadRequestException("End date must be on or after the start date");
        }
        policy.setProvider(r.provider());
        policy.setStartDate(r.startDate());
        policy.setEndDate(r.endDate());
        policy.setTotalCoverageAmount(r.totalCoverageAmount());
    }
}
