package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.StaffRequest;
import com.artgallery.dto.StaffResponse;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Staff;
import com.artgallery.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Staff}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> list(Pageable pageable) {
        log.debug("Listing staff {}", pageable);
        return PageResponse.from(staffRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> search(String term, Pageable pageable) {
        log.debug("Searching staff for '{}'", term);
        return PageResponse.from(staffRepository
                .findByNameContainingIgnoreCaseOrRoleContainingIgnoreCase(term, term, pageable)
                .map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        return mapper.toResponse(findStaff(id));
    }

    public StaffResponse create(StaffRequest request) {
        Staff staff = new Staff();
        apply(staff, request);
        Staff saved = staffRepository.save(staff);
        log.info("Created staff id={} name='{}'", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = findStaff(id);
        apply(staff, request);
        Staff saved = staffRepository.save(staff);
        log.info("Updated staff id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Staff staff = findStaff(id);
        staffRepository.delete(staff);
        log.info("Deleted staff id={}", id);
    }

    private Staff findStaff(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
    }

    private void apply(Staff staff, StaffRequest r) {
        staff.setName(r.name());
        staff.setRole(r.role());
        staff.setHireDate(r.hireDate());
        staff.setCertificationLevel(r.certificationLevel());
    }
}
