package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.InsuranceRequest;
import com.artgallery.dto.InsuranceResponse;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Insurance;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.InsurancePolicyRepository;
import com.artgallery.repository.InsuranceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Insurance}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InsuranceService {

    private final InsuranceRepository insuranceRepository;
    private final ArtworkRepository artworkRepository;
    private final InsurancePolicyRepository policyRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<InsuranceResponse> list(Pageable pageable) {
        log.debug("Listing insurances {}", pageable);
        return PageResponse.from(insuranceRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<InsuranceResponse> listByArtwork(Long artworkId, Pageable pageable) {
        return PageResponse.from(insuranceRepository.findByArtworkId(artworkId, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public InsuranceResponse get(Long id) {
        return mapper.toResponse(findInsurance(id));
    }

    public InsuranceResponse create(InsuranceRequest request) {
        Insurance insurance = new Insurance();
        apply(insurance, request);
        Insurance saved = insuranceRepository.save(insurance);
        log.info("Created insurance id={} artworkId={}", saved.getId(), request.artworkId());
        return mapper.toResponse(saved);
    }

    public InsuranceResponse update(Long id, InsuranceRequest request) {
        Insurance insurance = findInsurance(id);
        apply(insurance, request);
        Insurance saved = insuranceRepository.save(insurance);
        log.info("Updated insurance id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Insurance insurance = findInsurance(id);
        insuranceRepository.delete(insurance);
        log.info("Deleted insurance id={}", id);
    }

    private Insurance findInsurance(Long id) {
        return insuranceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance", id));
    }

    private void apply(Insurance insurance, InsuranceRequest r) {
        insurance.setInsuredAmount(r.insuredAmount());
        insurance.setArtwork(artworkRepository.findById(r.artworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Artwork", r.artworkId())));
        insurance.setPolicy(policyRepository.findById(r.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("InsurancePolicy", r.policyId())));
    }
}
