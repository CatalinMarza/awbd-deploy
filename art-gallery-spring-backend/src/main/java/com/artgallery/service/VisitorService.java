package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.VisitorRequest;
import com.artgallery.dto.VisitorResponse;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Visitor;
import com.artgallery.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * Business logic for Visitor.
 *
 * The join date is not displayed in the UI.
 * On create, it is generated if the request does not provide one.
 * On update, the existing join date is preserved if the request does not provide one.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<VisitorResponse> list(Pageable pageable) {
        log.debug("Listing visitors {}", pageable);

        return PageResponse.from(
                visitorRepository
                        .findAll(pageable)
                        .map(mapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitorResponse> search(String term, Pageable pageable) {
        log.debug("Searching visitors for '{}'", term);

        return PageResponse.from(
                visitorRepository
                        .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable)
                        .map(mapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public VisitorResponse get(Long id) {
        return mapper.toResponse(findVisitor(id));
    }

    public VisitorResponse create(VisitorRequest request) {
        if (StringUtils.hasText(request.email())
                && visitorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException(
                    "A visitor with email '%s' already exists".formatted(request.email())
            );
        }

        Visitor visitor = new Visitor();

        apply(visitor, request);
        visitor.setJoinDate(request.joinDate() != null ? request.joinDate() : LocalDate.now());

        Visitor saved = visitorRepository.save(visitor);

        log.info("Created visitor id={} name='{}'", saved.getId(), saved.getName());

        return mapper.toResponse(saved);
    }

    public VisitorResponse update(Long id, VisitorRequest request) {
        Visitor visitor = findVisitor(id);

        apply(visitor, request);

        if (request.joinDate() != null) {
            visitor.setJoinDate(request.joinDate());
        } else if (visitor.getJoinDate() == null) {
            visitor.setJoinDate(LocalDate.now());
        }

        Visitor saved = visitorRepository.save(visitor);

        log.info("Updated visitor id={}", id);

        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Visitor visitor = findVisitor(id);

        visitorRepository.delete(visitor);

        log.info("Deleted visitor id={}", id);
    }

    private Visitor findVisitor(Long id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", id));
    }

    private void apply(Visitor visitor, VisitorRequest request) {
        visitor.setName(request.name());
        visitor.setEmail(request.email());
        visitor.setPhone(request.phone());
        visitor.setMembershipType(request.membershipType());
    }
}